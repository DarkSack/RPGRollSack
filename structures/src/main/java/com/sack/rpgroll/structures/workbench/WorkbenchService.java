package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.common.integration.VaultEconomy;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.config.StructuresSettings;
import com.sack.rpgroll.structures.config.WorkbenchSettings;
import com.sack.rpgroll.structures.config.WorkbenchSettings.Cost;
import com.sack.rpgroll.structures.config.WorkbenchSettings.TierSettings;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.storage.WorkbenchRepository;
import com.sack.rpgroll.structures.workbench.BreakRules.BreakDecision;
import com.sack.rpgroll.structures.workbench.UpgradeRules.UpgradeCheck;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mesas en el mundo: caché en memoria (id y posición) respaldada por el repositorio. La caché se toca en el
 * hilo principal; la base de datos corre en su hilo y los resultados vuelven al hilo principal por {@code main}.
 * Aquí no se tocan bloques ni entidades (salvo el refresco de {@link WorkbenchDisplay} al mejorar): eso lo
 * hace el listener.
 */
public final class WorkbenchService {

    public enum PlaceResult { OK, NOT_GENUINE, NO_PERMISSION, OCCUPIED, ERROR }

    /** Resultado de dar o quitar acceso: hecho, ya estaba así, o quien lo pide no gestiona la mesa. */
    public enum AccessChange { DONE, UNCHANGED, DENIED }

    public static final String PERM_PLACE = "rpgrollstructures.workbench.place";
    public static final String PERM_BREAK = "rpgrollstructures.workbench.break";
    public static final String PERM_BYPASS = "rpgrollstructures.bypass";

    private final WorkbenchRepository repo;
    private final WorkbenchItems items;
    private final SerialSigner signer;
    private final LangManager lang;
    private final Supplier<WorkbenchSettings> workbench;
    private final Supplier<StructuresSettings> structures;
    private final ProjectsView projects;
    private final WorkbenchDisplay display;
    private final Predicate<String> worldExists;
    private final Executor main;
    private final Logger log;

    private final Map<UUID, WorkbenchRecord> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> byPos = new ConcurrentHashMap<>();
    /** Mesas con posición de cada chunk ({@code mundo:cx,cz}), para no recorrerlas todas al cargar uno. */
    private final Map<String, Set<UUID>> byChunk = new ConcurrentHashMap<>();
    /** Candado por mesa: mejoras en curso. */
    private final Set<UUID> upgrading = ConcurrentHashMap.newKeySet();
    /** Mesas que se están rompiendo: siguen en la caché (protegidas) hasta que la base confirme. */
    private final Set<UUID> breaking = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean vaultWarned = new AtomicBoolean();
    /** Cómo se fabrica el ítem devuelto en un reembolso; las pruebas lo cambian (ItemStack real pide servidor). */
    BiFunction<Material, Integer, ItemStack> stackFactory = ItemStack::new;

    public WorkbenchService(WorkbenchRepository repo, WorkbenchItems items, SerialSigner signer, LangManager lang,
                            Supplier<WorkbenchSettings> workbench, Supplier<StructuresSettings> structures,
                            ProjectsView projects, WorkbenchDisplay display, Predicate<String> worldExists,
                            Executor main, Logger log) {
        this.repo = repo;
        this.items = items;
        this.signer = signer;
        this.lang = lang;
        this.workbench = workbench;
        this.structures = structures;
        this.projects = projects;
        this.display = display;
        this.worldExists = worldExists;
        this.main = main;
        this.log = log;
    }

    private static String key(String world, int x, int y, int z) {
        return world + ":" + x + "," + y + "," + z;
    }

    private static String key(WorkbenchRecord r) {
        return key(r.world(), r.x(), r.y(), r.z());
    }

    private static String key(Block b) {
        return key(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
    }

    private static String chunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + "," + chunkZ;
    }

    private static String chunkKey(WorkbenchRecord r) {
        return chunkKey(r.world(), r.x() >> 4, r.z() >> 4);
    }

    /** Da posición a la mesa; false si ya hay otra en ese bloque. */
    private boolean addPos(WorkbenchRecord r) {
        if (byPos.putIfAbsent(key(r), r.id()) != null) {
            return false;
        }
        byChunk.computeIfAbsent(chunkKey(r), k -> ConcurrentHashMap.newKeySet()).add(r.id());
        return true;
    }

    private void removePos(WorkbenchRecord r) {
        if (byPos.remove(key(r), r.id())) {
            byChunk.computeIfPresent(chunkKey(r), (k, ids) -> {
                ids.remove(r.id());
                return ids.isEmpty() ? null : ids;
            });
        }
    }

    /**
     * Pasa el resultado al hilo principal, también si falla: {@code thenApplyAsync} con un origen fallido terminaría
     * en el hilo de la base de datos y el menú mandaría mensajes desde ahí.
     */
    private <T, R> CompletableFuture<R> onMain(CompletableFuture<T> f, Function<T, R> then) {
        return f.handleAsync((v, err) -> {
            if (err != null) {
                throw err instanceof CompletionException ce ? ce : new CompletionException(err);
            }
            return then.apply(v);
        }, main);
    }

    // ---- caché ----

    /**
     * Carga todas las filas. Las de un mundo que aún no está cargado (WorldForge o Multiverse lo abren después de
     * onEnable) quedan en memoria sin posición y sin marcarse: {@link #worldLoaded} las indexa. Una fila
     * ilegible se avisa y se salta.
     */
    public void load() {
        List<WorkbenchRecord> rows = repo.all(bad -> log.warning(lang.raw("workbench.warning.bad-row", "id", bad)))
                .join();
        for (WorkbenchRecord r : rows) {
            if (r.orphan()) {
                byId.put(r.id(), r);
            } else if (!worldExists.test(r.world())) {
                log.warning(lang.raw("workbench.warning.world-missing", "id", r.id(), "world", r.world()));
                byId.put(r.id(), r);
            } else {
                index(r);
            }
        }
    }

    /** Un mundo acaba de cargar: sus mesas pasan a tener posición. */
    public void worldLoaded(String world) {
        for (WorkbenchRecord r : byId.values()) {
            if (!r.orphan() && r.world().equals(world)) {
                addPos(r);
            }
        }
    }

    private void index(WorkbenchRecord r) {
        byId.put(r.id(), r);
        addPos(r);
    }

    /** Marca la mesa como huérfana (fila conservada) y la saca del índice por posición; un fallo de la base se avisa. */
    public void markOrphan(WorkbenchRecord r) {
        removePos(r);
        byId.put(r.id(), orphaned(r));
        repo.markOrphan(r.id()).whenComplete((ok, err) -> {
            if (err != null) {
                log.log(Level.SEVERE, "No se pudo marcar como huérfana la mesa " + r.id(), err);
            }
        });
    }

    private static WorkbenchRecord orphaned(WorkbenchRecord r) {
        return new WorkbenchRecord(r.id(), r.owner(), r.world(), r.x(), r.y(), r.z(), r.tier(), r.serial(), true,
                r.createdAt());
    }

    public Optional<WorkbenchRecord> at(Block b) {
        UUID id = byPos.get(key(b));
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    public Optional<WorkbenchRecord> byId(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** Mesas activas de un chunk. */
    public List<WorkbenchRecord> inChunk(String world, int chunkX, int chunkZ) {
        List<WorkbenchRecord> out = new ArrayList<>();
        for (UUID id : byChunk.getOrDefault(chunkKey(world, chunkX, chunkZ), Set.of())) {
            WorkbenchRecord r = byId.get(id);
            if (r != null && !r.orphan()) {
                out.add(r);
            }
        }
        return out;
    }

    // ---- colocar ----

    /** Comprobaciones síncronas; el listener cancela el evento si no es OK. */
    public PlaceResult validate(Player p, ItemStack item, Block at) {
        Optional<WorkbenchStamp> stamp = items.read(item);
        if (stamp.isEmpty() || !items.isGenuine(stamp.get(), signer)) {
            return PlaceResult.NOT_GENUINE;
        }
        if (!p.hasPermission(PERM_PLACE) && !p.hasPermission(PERM_BYPASS)) {
            return PlaceResult.NO_PERMISSION;
        }
        if (alive(byId.get(stamp.get().id())) || byPos.containsKey(key(at))) {
            return PlaceResult.OCCUPIED;
        }
        return PlaceResult.OK;
    }

    /**
     * Una huérfana no está en el mundo: su ítem auténtico (devuelto tras un {@code cancelPlacement} fallido, o en
     * manos de alguien tras perder el bloque) puede volver a colocarla. Solo una mesa viva con ese id ocupa.
     */
    private static boolean alive(WorkbenchRecord r) {
        return r != null && !r.orphan();
    }

    /**
     * Registra la mesa con la identidad del ítem (no se emite otra): id, nivel y serie salen de su sello.
     * La mesa entra en la caché ya, para que nadie la coloque dos veces mientras se escribe en la base.
     */
    public CompletableFuture<PlaceResult> place(Player p, ItemStack item, Block at) {
        PlaceResult v = validate(p, item, at);
        if (v != PlaceResult.OK) {
            return CompletableFuture.completedFuture(v);
        }
        WorkbenchStamp s = items.read(item).orElseThrow();
        WorkbenchRecord rec = new WorkbenchRecord(s.id(), p.getUniqueId(), at.getWorld().getName(), at.getX(),
                at.getY(), at.getZ(), s.tier(), s.serial(), false, System.currentTimeMillis());
        WorkbenchRecord orphan = byId.get(rec.id());
        if (alive(orphan)) {
            return CompletableFuture.completedFuture(PlaceResult.OCCUPIED);
        }
        boolean claimed = orphan == null ? byId.putIfAbsent(rec.id(), rec) == null : byId.replace(rec.id(), orphan, rec);
        if (!claimed) {
            return CompletableFuture.completedFuture(PlaceResult.OCCUPIED);
        }
        if (!addPos(rec)) {
            restore(rec, orphan);
            return CompletableFuture.completedFuture(PlaceResult.OCCUPIED);
        }
        CompletableFuture<Void> write = orphan == null ? repo.insert(rec) : repo.reclaim(rec);
        return write.handleAsync((ok, err) -> {
            if (err != null) {
                removePos(rec);
                restore(rec, orphan);
                log.log(Level.SEVERE, "No se pudo guardar la mesa " + rec.id(), err);
                return PlaceResult.ERROR;
            }
            try {
                projects.benchPlaced(p, rec.id()); // reconecta su obra si estaba en pausa por la mesa
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "No se pudo reanudar la obra de la mesa " + rec.id(), e);
            }
            return PlaceResult.OK;
        }, main);
    }

    /** Deshace la entrada de la caché de una colocación fallida: vuelve la huérfana que había, o nada. */
    private void restore(WorkbenchRecord placed, WorkbenchRecord orphan) {
        if (orphan == null) {
            byId.remove(placed.id(), placed);
        } else {
            byId.replace(placed.id(), placed, orphan);
        }
    }

    /**
     * Deshace un registro cuyo bloque no llegó a colocarse: fila y caché fuera. Si la base falla, la fila sigue ahí y
     * la mesa queda en la caché como huérfana (sin posición), igual que quedaría tras reiniciar.
     */
    public CompletableFuture<Void> cancelPlacement(UUID id) {
        WorkbenchRecord rec = byId.get(id);
        if (rec != null) {
            removePos(rec); // nada se apoya ya en ese bloque
        }
        return repo.remove(id).handleAsync((ok, err) -> {
            if (err == null) {
                byId.remove(id);
            } else {
                log.log(Level.SEVERE, "No se pudo deshacer el registro de la mesa " + id + "; queda como huérfana", err);
                if (rec != null) {
                    markOrphan(rec);
                }
            }
            return null;
        }, main);
    }

    // ---- romper ----

    public BreakDecision breakDecision(Player p, WorkbenchRecord bench) {
        boolean bypass = p.hasPermission(PERM_BYPASS);
        boolean manage = AccessRules.canManage(p.getUniqueId(), bench, Set.of(), bypass)
                && (bypass || p.hasPermission(PERM_BREAK));
        return BreakRules.decide(structures.get().breakPolicy(), projects.hasActiveProject(bench.id()), manage);
    }

    /** Por qué no puede romperla (clave de lang): no es suya, le falta el permiso o tiene una obra activa. */
    public String breakDeniedKey(Player p, WorkbenchRecord bench) {
        boolean bypass = p.hasPermission(PERM_BYPASS);
        if (!AccessRules.canManage(p.getUniqueId(), bench, Set.of(), bypass)) {
            return "workbench.break.denied";
        }
        if (!bypass && !p.hasPermission(PERM_BREAK)) {
            return "workbench.break.no-permission";
        }
        return "workbench.break.has-project";
    }

    /**
     * Cómo acabó un intento de romper. {@code DROPPED}: fuera de la base y de la caché, con su ítem. {@code PAUSED}
     * ({@code break-policy: pause}): la fila queda huérfana, sale su ítem y la obra espera a que se vuelva a colocar.
     * {@code CANCELLED} ({@code cancel}): la obra se canceló y la mesa salió como en {@code DROPPED}.
     * {@code CANCEL_FAILED}: la cancelación no se confirmó y la mesa sigue. {@code GONE}: la fila ya no estaba (otro
     * proceso la borró); la mesa sale de la caché sin ítem y el listener quita el bloque. {@code UPGRADING}: hay una
     * mejora en curso. {@code NOTHING}: no hay mesa, no puede o ya se está rompiendo.
     */
    public enum BreakStatus { DROPPED, PAUSED, CANCELLED, CANCEL_FAILED, GONE, UPGRADING, NOTHING }

    public record BreakResult(BreakStatus status, Optional<ItemStack> dropped) {
        static BreakResult of(BreakStatus status) {
            return new BreakResult(status, Optional.empty());
        }
    }

    /**
     * Quita la mesa y devuelve su ítem, con la misma identidad. La mesa queda marcada como "rompiéndose"
     * (un segundo intento no hace nada) pero sigue en la caché, protegida, hasta que la base confirme; si la
     * base falla, sigue ahí, protegida. Si la fila ya no estaba, se trata como inexistente ({@code GONE}): no se
     * suelta ningún ítem (no hay con qué demostrar que no se duplica). Se niega mientras haya una mejora en curso.
     * Con obra activa, según {@code break-policy}: {@code pause} deja la fila huérfana y {@code cancel} cancela antes la
     * obra.
     */
    public CompletableFuture<BreakResult> breakBench(Player p, Block at) {
        Optional<WorkbenchRecord> found = at(at);
        BreakDecision decision = found.isEmpty() ? BreakDecision.DENY : breakDecision(p, found.get());
        if (decision == BreakDecision.DENY) {
            return CompletableFuture.completedFuture(BreakResult.of(BreakStatus.NOTHING));
        }
        WorkbenchRecord rec = found.get();
        // Con una mejora en curso no se rompe: el cobro y el nivel del ítem se cruzarían.
        if (upgrading.contains(rec.id())) {
            return CompletableFuture.completedFuture(BreakResult.of(BreakStatus.UPGRADING));
        }
        if (!breaking.add(rec.id())) {
            return CompletableFuture.completedFuture(BreakResult.of(BreakStatus.NOTHING));
        }
        return switch (decision) {
            case ALLOW_PAUSE -> orphanOnBreak(p, rec);
            case ALLOW_CANCEL -> cancelThenRemove(p, rec);
            default -> removeOnBreak(rec, BreakStatus.DROPPED);
        };
    }

    /** Borra la fila y suelta el ítem ({@code done}); suelta el candado {@code breaking}. */
    private CompletableFuture<BreakResult> removeOnBreak(WorkbenchRecord rec, BreakStatus done) {
        // La mesa sigue en la caché (at() la ve y las explosiones/pistones la respetan) hasta confirmar.
        return repo.remove(rec.id()).handleAsync((removed, err) -> {
            try {
                if (err != null) {
                    throw new CompletionException(err);
                }
                byId.remove(rec.id(), rec);
                removePos(rec);
                if (!removed) {
                    return gone(rec);
                }
            } finally {
                breaking.remove(rec.id());
            }
            return new BreakResult(done, Optional.of(itemOf(rec)));
        }, main);
    }

    /**
     * {@code pause}: la fila se conserva como huérfana (sin posición, así otra mesa puede ir en ese bloque) y su obra
     * queda atada a ella; el ítem sale con el mismo sello. Suelta el candado {@code breaking}.
     */
    private CompletableFuture<BreakResult> orphanOnBreak(Player p, WorkbenchRecord rec) {
        return repo.markOrphan(rec.id()).handleAsync((marked, err) -> {
            try {
                if (err != null) {
                    throw new CompletionException(err);
                }
                removePos(rec);
                if (!marked) {
                    byId.remove(rec.id(), rec);
                    return gone(rec);
                }
                byId.put(rec.id(), orphaned(rec));
            } finally {
                breaking.remove(rec.id());
            }
            try {
                projects.benchRemoved(p, rec.id());
            } catch (RuntimeException e) {
                log.log(Level.SEVERE, "No se pudo pausar la obra de la mesa " + rec.id(), e);
            }
            return new BreakResult(BreakStatus.PAUSED, Optional.of(itemOf(rec)));
        }, main);
    }

    /**
     * {@code cancel}: primero la obra (devolución al dueño de la obra); solo si la base lo confirma se rompe la mesa.
     * Si no, la mesa sigue y se suelta el candado {@code breaking}.
     */
    private CompletableFuture<BreakResult> cancelThenRemove(Player p, WorkbenchRecord rec) {
        CompletableFuture<Boolean> cancel;
        try {
            cancel = projects.cancelFor(p, rec.id());
        } catch (RuntimeException e) {
            cancel = CompletableFuture.failedFuture(e);
        }
        return cancel.handleAsync((ok, err) -> {
            if (err == null && Boolean.TRUE.equals(ok)) {
                return removeOnBreak(rec, BreakStatus.CANCELLED);
            }
            breaking.remove(rec.id());
            if (err != null) {
                log.log(Level.SEVERE, "No se pudo cancelar la obra de la mesa " + rec.id(), err);
            }
            return CompletableFuture.completedFuture(BreakResult.of(BreakStatus.CANCEL_FAILED));
        }, main).thenCompose(f -> f);
    }

    /** La fila ya no estaba: sale de la caché, así el siguiente clic ya no la encuentra y este aviso no se repite. */
    private BreakResult gone(WorkbenchRecord rec) {
        log.warning("La mesa " + rec.id() + " ya no estaba en la base de datos al romperla;"
                + " se quita del mundo sin soltar ítem.");
        return BreakResult.of(BreakStatus.GONE);
    }

    /** El ítem de la mesa, con su misma identidad. */
    private ItemStack itemOf(WorkbenchRecord rec) {
        WorkbenchStamp stamp = new WorkbenchStamp(rec.id(), rec.tier(), rec.serial(),
                signer.sign(rec.id(), rec.tier(), rec.serial()));
        return items.create(stamp, lang, workbench.get().tiers().get(rec.tier()));
    }

    // ---- acceso ----

    public CompletableFuture<Boolean> canUse(Player p, WorkbenchRecord bench) {
        boolean bypass = p.hasPermission(PERM_BYPASS);
        if (AccessRules.canManage(p.getUniqueId(), bench, Set.of(), bypass)) {
            return CompletableFuture.completedFuture(true);
        }
        return onMain(repo.trusted(bench.id()), trusted -> AccessRules.canUse(p.getUniqueId(), bench, trusted, false));
    }

    /** Los jugadores de confianza de una mesa; el resultado (también el fallo) vuelve al hilo principal. */
    public CompletableFuture<Set<UUID>> trusted(UUID bench) {
        return onMain(repo.trusted(bench), t -> t);
    }

    /** {@code UNCHANGED} si ya tenía acceso. */
    public CompletableFuture<AccessChange> trust(Player owner, UUID bench, UUID target) {
        return changeTrust(owner, bench, () -> repo.trust(bench, target));
    }

    /** {@code UNCHANGED} si no lo tenía. */
    public CompletableFuture<AccessChange> untrust(Player owner, UUID bench, UUID target) {
        return changeTrust(owner, bench, () -> repo.untrust(bench, target));
    }

    private CompletableFuture<AccessChange> changeTrust(Player owner, UUID bench,
                                                        Supplier<CompletableFuture<Boolean>> op) {
        WorkbenchRecord rec = byId.get(bench);
        if (rec == null
                || !AccessRules.canManage(owner.getUniqueId(), rec, Set.of(), owner.hasPermission(PERM_BYPASS))) {
            return CompletableFuture.completedFuture(AccessChange.DENIED);
        }
        return onMain(op.get(), changed -> changed ? AccessChange.DONE : AccessChange.UNCHANGED);
    }

    // ---- mejorar ----

    /**
     * Mejora la mesa un nivel. En el hilo principal: comprueba, quita ítems y dinero y escribe el nivel con
     * {@code repo.upgrade} (condicional). Si la escritura no cambia nada o falla, devuelve lo cobrado.
     * Una mejora en curso ignora los clics repetidos (devuelve {@link UpgradeRules.MaxTier}, sin efecto), y
     * también un menú viejo: si la mesa ya no está en {@code expectedFrom} (otro gestor la mejoró), no se cobra
     * ni se cambia nada.
     */
    public CompletableFuture<UpgradeCheck> upgrade(Player p, UUID benchId, WorkbenchTier expectedFrom) {
        WorkbenchRecord bench = byId.get(benchId);
        boolean bypass = p.hasPermission(PERM_BYPASS);
        if (bench == null || bench.orphan()
                || !AccessRules.canManage(p.getUniqueId(), bench, Set.of(), bypass)) {
            return CompletableFuture.completedFuture(new UpgradeRules.NoPermission());
        }
        if (bench.tier() != expectedFrom || breaking.contains(benchId) || !upgrading.add(benchId)) {
            return CompletableFuture.completedFuture(new UpgradeRules.MaxTier());
        }
        boolean handedOff = false;
        try {
            Optional<WorkbenchTier> nextTier = bench.tier().next();
            if (nextTier.isEmpty()) {
                return CompletableFuture.completedFuture(new UpgradeRules.MaxTier());
            }
            TierSettings next = workbench.get().tiers().get(nextTier.get());
            double price = next.upgradeMoney();
            Economy eco = price > 0 ? VaultEconomy.get().orElse(null) : null;
            if (price > 0 && eco == null && vaultWarned.compareAndSet(false, true)) {
                log.warning(lang.raw("workbench.warning.vault-missing"));
            }
            boolean charge = price > 0 && eco != null;
            Map<Material, Integer> cost = materials(next.upgradeCost());
            UpgradeCheck check = UpgradeRules.check(bench.tier(), next, count(p.getInventory(), next.upgradeCost()),
                    charge ? eco.getBalance(p) : Double.MAX_VALUE,
                    bypass || p.hasPermission("rpgrollstructures.tier." + nextTier.get().level()));
            if (!(check instanceof UpgradeRules.Ok)) {
                return CompletableFuture.completedFuture(check);
            }
            take(p.getInventory(), cost);
            if (charge && !eco.withdrawPlayer(p, price).transactionSuccess()) {
                give(p, cost);
                return CompletableFuture.completedFuture(new UpgradeRules.NotEnoughMoney(price));
            }
            WorkbenchTier to = nextTier.get();
            handedOff = true;
            return repo.upgrade(benchId, bench.tier(), to).handleAsync((ok, err) -> {
                try {
                    if (err == null && ok) {
                        WorkbenchRecord now = new WorkbenchRecord(bench.id(), bench.owner(), bench.world(),
                                bench.x(), bench.y(), bench.z(), to, bench.serial(), false, bench.createdAt());
                        byId.replace(benchId, bench, now);
                        display.refresh(now);
                        return (UpgradeCheck) new UpgradeRules.Ok();
                    }
                    give(p, cost);
                    if (charge) {
                        eco.depositPlayer(p, price);
                    }
                    if (err != null) {
                        log.log(Level.SEVERE, "No se pudo mejorar la mesa " + benchId, err);
                        throw new CompletionException(err);
                    }
                    return (UpgradeCheck) new UpgradeRules.MaxTier();
                } finally {
                    upgrading.remove(benchId);
                }
            }, main);
        } finally {
            if (!handedOff) {
                upgrading.remove(benchId);
            }
        }
    }

    /** Solo ítems vanilla ({@code minecraft:id}); un id que no es material nunca se cumple. */
    private static Map<Material, Integer> materials(List<Cost> costs) {
        Map<Material, Integer> out = new EnumMap<>(Material.class);
        for (Cost c : costs) {
            Material m = Material.matchMaterial(c.itemId());
            if (m != null) {
                out.merge(m, c.amount(), Integer::sum);
            }
        }
        return out;
    }

    /**
     * El coste solo se paga con ítems vanilla sin nada propio: un ítem de otro plugin con el mismo material (una
     * reliquia de RPGRoll-Items es una NETHER_STAR) ni cuenta ni se cobra. El reembolso da ítems simples.
     */
    private static boolean plain(ItemStack s, Material m) {
        return s != null && s.getType() == m && !s.hasItemMeta();
    }

    /** Lo que el jugador lleva de cada coste, solo en ítems simples. El menú de mejoras usa lo mismo. */
    public static Map<String, Integer> count(PlayerInventory inv, List<Cost> costs) {
        Map<String, Integer> has = new LinkedHashMap<>();
        for (Cost c : costs) {
            Material m = Material.matchMaterial(c.itemId());
            int n = 0;
            if (m != null) {
                for (ItemStack s : inv.getContents()) {
                    if (plain(s, m)) {
                        n += s.getAmount();
                    }
                }
            }
            has.put(c.itemId(), n);
        }
        return has;
    }

    private static void take(PlayerInventory inv, Map<Material, Integer> cost) {
        for (Map.Entry<Material, Integer> e : cost.entrySet()) {
            int left = e.getValue();
            ItemStack[] slots = inv.getContents();
            for (int i = 0; i < slots.length && left > 0; i++) {
                ItemStack s = slots[i];
                if (!plain(s, e.getKey())) {
                    continue;
                }
                int took = Math.min(left, s.getAmount());
                left -= took;
                if (took == s.getAmount()) {
                    inv.setItem(i, null);
                } else {
                    s.setAmount(s.getAmount() - took);
                }
            }
        }
    }

    private void give(Player p, Map<Material, Integer> cost) {
        for (Map.Entry<Material, Integer> e : cost.entrySet()) {
            Map<Integer, ItemStack> rest = p.getInventory().addItem(stackFactory.apply(e.getKey(), e.getValue()));
            rest.values().forEach(r -> p.getWorld().dropItemNaturally(p.getLocation(), r));
        }
    }
}
