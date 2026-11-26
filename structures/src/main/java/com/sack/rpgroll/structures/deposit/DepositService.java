package com.sack.rpgroll.structures.deposit;

import com.sack.rpgroll.structures.catalog.MaterialSpec;
import com.sack.rpgroll.structures.catalog.StructureRegistry;
import com.sack.rpgroll.structures.material.MaterialRegistry;
import com.sack.rpgroll.structures.project.DepositMath;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectState;
import com.sack.rpgroll.structures.storage.MaterialRow;
import com.sack.rpgroll.structures.storage.Movement;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Depósito de materiales: quita del inventario, apunta en el libro y, si la escritura falla, devuelve lo quitado.
 * Todo el estado en memoria se toca solo en el hilo principal.
 */
public final class DepositService {

    private final ProjectRepository repo;
    private final StructureRegistry structures;
    private final MaterialRegistry materials;
    private final ReturnService returns;
    /** Busca al jugador conectado ahora (null si no está): puede ser otra entidad si reconectó. */
    private final Function<UUID, Player> online;
    private final Consumer<Runnable> main;
    private final Logger log;
    /** Jugadores con un depósito en curso (hilo principal). */
    private final Set<UUID> depositing = new HashSet<>();
    /** Obras ya avisadas de materiales sin fila (una vez por obra). */
    private final Set<UUID> warnedExtra = new HashSet<>();

    public DepositService(ProjectRepository repo, StructureRegistry structures, MaterialRegistry materials,
                          ReturnService returns, Function<UUID, Player> online, Consumer<Runnable> main,
                          Logger log) {
        this.repo = repo;
        this.structures = structures;
        this.materials = materials;
        this.returns = returns;
        this.online = online;
        this.main = main;
        this.log = log;
    }

    /**
     * Hilo principal. {@code onlyKey} null = todos los materiales; {@code maxPerKey} limita lo que se toma de cada uno.
     * Un depósito mientras otro sigue en curso (del jugador o de la obra) se descarta con BUSY.
     */
    public CompletableFuture<DepositResult> deposit(Player p, Project project, String onlyKey, int maxPerKey) {
        UUID id = p.getUniqueId();
        if (project.busy() || depositing.contains(id)) {
            return CompletableFuture.completedFuture(DepositResult.BUSY);
        }
        if (project.state() != ProjectState.WAITING_MATERIALS) {
            return CompletableFuture.completedFuture(DepositResult.WRONG_STATE);
        }
        List<MaterialSpec> specs = structures.get(project.structure()).map(d -> d.materials()).orElse(null);
        if (specs == null) {
            return CompletableFuture.completedFuture(DepositResult.ERROR);
        }
        List<String> extra = DepositMath.missingRows(specs, project.materials());
        if (!extra.isEmpty() && warnedExtra.add(project.id())) {
            log.warning("La obra " + project.id() + " no tiene " + extra + " (la estructura cambió al recargar): no"
                    + " formaban parte de su precio y se dan por cumplidos.");
        }
        Inventory inv = p.getInventory();
        Map<String, Integer> available = new LinkedHashMap<>();
        for (MaterialSpec spec : specs) {
            available.putIfAbsent(spec.key(), Math.min(count(inv, spec), maxPerKey));
        }
        Map<String, Integer> take = DepositMath.toTake(specs, project.materials(), available, onlyKey);
        if (take.isEmpty()) {
            // Obra completa que no llegó a READY (por ejemplo, un guardado fallido antes): se sana aquí.
            if (DepositMath.complete(specs, project.materials())) {
                markReady(project);
                return CompletableFuture.completedFuture(DepositResult.OK);
            }
            return CompletableFuture.completedFuture(DepositResult.NOTHING_TO_TAKE);
        }

        depositing.add(id);
        project.setBusy(true);
        // Copia de lo quitado (pilas reales, con su meta) por si hay que devolverlo tal cual.
        Map<String, List<ItemStack>> removed = new LinkedHashMap<>();
        try {
            for (MaterialSpec spec : specs) {
                if (take.containsKey(spec.key()) && !removed.containsKey(spec.key())) {
                    List<ItemStack> list = new ArrayList<>();
                    removed.put(spec.key(), list);
                    remove(inv, spec, take.get(spec.key()), list);
                }
            }
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Fallo al quitar materiales de " + id + ": se devuelve lo ya quitado", t);
            removed.values().forEach(l -> giveBack(id, p, l));
            release(id, project);
            return CompletableFuture.completedFuture(DepositResult.ERROR);
        }
        // Lo realmente quitado manda: con specs solapados puede ser menos que lo contado.
        Map<String, Integer> got = new LinkedHashMap<>();
        removed.forEach((key, l) -> {
            int n = l.stream().mapToInt(ItemStack::getAmount).sum();
            if (n > 0) {
                got.put(key, n);
            }
        });
        if (got.isEmpty()) {
            release(id, project);
            return CompletableFuture.completedFuture(DepositResult.NOTHING_TO_TAKE);
        }
        try {
            got.forEach((key, n) -> adjust(project, key, n));
            Map<String, CompletableFuture<Boolean>> writes = new LinkedHashMap<>();
            got.forEach((key, n) -> writes.put(key, repo
                    .applyMovement(new Movement(UUID.randomUUID(), project.id(), id, Movement.Kind.DEPOSIT, key, n))
                    .exceptionally(e -> {
                        log.log(Level.SEVERE, "Falló el apunte de depósito de " + id + " en " + project.id(), e);
                        return false;
                    })));

            CompletableFuture<DepositResult> result = new CompletableFuture<>();
            CompletableFuture.allOf(writes.values().toArray(new CompletableFuture[0])).whenComplete((v, e) ->
                    main.accept(() -> {
                        try {
                            result.complete(finish(p, project, specs, got, removed, writes));
                        } catch (Throwable t) {
                            log.log(Level.SEVERE, "Depósito de " + id + " en " + project.id() + " con error", t);
                            result.complete(DepositResult.ERROR);
                        } finally {
                            release(id, project);
                        }
                    }));
            return result;
        } catch (Throwable t) {
            // Falló antes de escribir: se deshace la memoria y se devuelve todo.
            log.log(Level.SEVERE, "Fallo al apuntar el depósito de " + id, t);
            got.forEach((key, n) -> adjust(project, key, -n));
            removed.values().forEach(l -> giveBack(id, p, l));
            release(id, project);
            return CompletableFuture.completedFuture(DepositResult.ERROR);
        }
    }

    private void release(UUID id, Project project) {
        depositing.remove(id);
        project.setBusy(false);
    }

    private void giveBack(UUID id, Player current, List<ItemStack> items) {
        if (items.isEmpty()) {
            return;
        }
        try {
            if (current == null) {
                returns.givePending(id, items);
            } else {
                returns.give(current, items);
            }
        } catch (Throwable t) {
            log.log(Level.SEVERE, "No se pudieron devolver " + items.size() + " pilas a " + id, t);
        }
    }

    private void markReady(Project project) {
        project.setState(ProjectState.READY);
        repo.saveProgress(project.id(), ProjectState.READY, project.cursor(), project.placed(), project.skipped(), null)
                .whenComplete((ok, e) -> {
                    if (e != null) {
                        log.log(Level.SEVERE, "No se pudo guardar READY de " + project.id(), e);
                    } else if (!ok) {
                        log.warning("READY de " + project.id() + " no se guardó (obra inexistente o ya final).");
                    }
                });
    }

    private DepositResult finish(Player p, Project project, List<MaterialSpec> specs, Map<String, Integer> got,
                                 Map<String, List<ItemStack>> removed, Map<String, CompletableFuture<Boolean>> writes) {
        // Quien depositó puede haber reconectado durante la escritura: manda la entidad de ahora.
        Player now = online.apply(p.getUniqueId());
        boolean any = false;
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, CompletableFuture<Boolean>> w : writes.entrySet()) {
            if (w.getValue().join()) {
                any = true;
            } else {
                failed.add(w.getKey());
            }
        }
        // Primero toda la memoria, luego las devoluciones: si una falla, el resto ya está bien.
        failed.forEach(key -> adjust(project, key, -got.get(key)));
        failed.forEach(key -> giveBack(p.getUniqueId(), now, removed.get(key)));
        if (any && now != null) {
            now.saveData();
        }
        // Lo escrito es duradero aunque otro apunte fallara: si ya está completo, READY.
        if (any && DepositMath.complete(specs, project.materials())) {
            markReady(project);
        }
        return failed.isEmpty() ? DepositResult.OK : DepositResult.ERROR;
    }

    /** Unidades del almacén (sin armadura puesta) que casan con cualquiera de {@code anyOf}. */
    private int count(Inventory inv, MaterialSpec spec) {
        int total = 0;
        for (ItemStack s : storage(inv)) {
            if (s != null && !s.isEmpty() && spec.anyOf().stream().anyMatch(m -> materials.matches(m, s))) {
                total += s.getAmount();
            }
        }
        return total;
    }

    private static ItemStack[] storage(Inventory inv) {
        return inv instanceof PlayerInventory pi ? pi.getStorageContents() : inv.getContents();
    }

    /** Quita hasta {@code amount} de las pilas que casan; cada pila quitada se anota en {@code out} al momento. */
    private void remove(Inventory inv, MaterialSpec spec, int amount, List<ItemStack> out) {
        ItemStack[] contents = storage(inv);
        for (int slot = 0; slot < contents.length && amount > 0; slot++) {
            ItemStack s = contents[slot];
            if (s == null || s.isEmpty() || spec.anyOf().stream().noneMatch(m -> materials.matches(m, s))) {
                continue;
            }
            int n = Math.min(amount, s.getAmount());
            ItemStack part = s.clone();
            part.setAmount(n);
            // Nunca se muta la pila de getStorageContents (en Paper es espejo del inventario real): se pone una copia.
            ItemStack rest = null;
            if (n < s.getAmount()) {
                rest = s.clone();
                rest.setAmount(s.getAmount() - n);
            }
            inv.setItem(slot, rest);
            out.add(part);
            amount -= n;
        }
    }

    /** Suma {@code delta} a lo depositado en memoria (mapa nuevo: el anterior puede ser inmutable). */
    static void adjust(Project project, String key, int delta) {
        Map<String, MaterialRow> rows = new LinkedHashMap<>(project.materials());
        MaterialRow r = rows.get(key);
        rows.put(key, new MaterialRow(r.required(), r.deposited() + delta, r.consumed()));
        project.setMaterials(rows);
    }
}
