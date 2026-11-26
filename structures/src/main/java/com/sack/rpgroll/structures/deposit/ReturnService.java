package com.sack.rpgroll.structures.deposit;

import com.sack.rpgroll.structures.material.MaterialRegistry;
import com.sack.rpgroll.structures.project.DepositMath;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.project.ProjectState;
import com.sack.rpgroll.structures.storage.MaterialRow;
import com.sack.rpgroll.structures.storage.Movement;
import com.sack.rpgroll.structures.storage.PendingRow;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Devuelve ítems al jugador (inventario y, lo que no quepa, pendientes; nunca al suelo) y mueve materiales fuera
 * de la obra: recuperar, cancelar y recoger pendientes. Regla: primero el libro, después la memoria y los ítems.
 * Lo que no queda apuntado en la base de datos no se entrega.
 */
public class ReturnService {

    private final ProjectRepository repo;
    private final MaterialRegistry materials;
    private final Consumer<Project> finisher;
    private final Function<UUID, Player> online;
    private final Consumer<Runnable> main;
    private final BiConsumer<Player, String> message;
    private final Function<ItemStack, byte[]> serializer;
    private final Function<byte[], ItemStack> deserializer;
    /** null: el dinero devuelto queda en pendientes hasta que alguien lo pague. */
    private final MoneyService money;
    private final Logger log;
    /** Jugadores con un recoger en curso (hilo principal). */
    private final Set<UUID> claiming = new HashSet<>();

    /**
     * @param finisher saca una obra cancelada de la memoria (ProjectService::finish)
     * @param online   busca al jugador conectado ahora (null si no está)
     * @param message  avisa a un jugador con una clave del lang
     * @param money    paga el dinero devuelto al cancelar (null: se queda en pendientes)
     */
    public ReturnService(ProjectRepository repo, MaterialRegistry materials, Consumer<Project> finisher,
                         Function<UUID, Player> online, Consumer<Runnable> main, BiConsumer<Player, String> message,
                         MoneyService money, Logger log) {
        this(repo, materials, finisher, online, main, message, ItemStack::serializeAsBytes,
                ItemStack::deserializeBytes, money, log);
    }

    /** Serializador y deserializador se cambian en pruebas: los de Bukkit necesitan servidor. */
    ReturnService(ProjectRepository repo, MaterialRegistry materials, Consumer<Project> finisher,
                  Function<UUID, Player> online, Consumer<Runnable> main, BiConsumer<Player, String> message,
                  Function<ItemStack, byte[]> serializer, Function<byte[], ItemStack> deserializer,
                  MoneyService money, Logger log) {
        this.repo = repo;
        this.materials = materials;
        this.finisher = finisher;
        this.online = online;
        this.main = main;
        this.message = message;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.money = money;
        this.log = log;
    }

    /**
     * Hilo principal. Cada pila que no cabe va a pendientes por separado. Un jugador desconectado no tiene
     * inventario fiable: todo va a pendientes.
     */
    public void give(Player p, List<ItemStack> items) {
        if (!p.isOnline()) {
            givePending(p.getUniqueId(), items);
            return;
        }
        for (ItemStack item : items) {
            addPending(p.getUniqueId(), p.getInventory().addItem(item).values());
        }
    }

    /** Todo a pendientes, una pila por fila (jugador que no está conectado). */
    public void givePending(UUID player, List<ItemStack> items) {
        addPending(player, items);
    }

    private void addPending(UUID id, Collection<ItemStack> items) {
        for (ItemStack r : items) {
            byte[] bytes = null;
            try {
                bytes = serializer.apply(r);
                repo.addPending(id, bytes).exceptionally(e -> {
                    lost(id, r, null, e);
                    return null;
                });
            } catch (Throwable t) {
                lost(id, r, bytes, t);
            }
        }
    }

    /** El ítem ya salió del libro y no quedó en ningún sitio: el log lleva todo para que un admin lo reponga. */
    private void lost(UUID id, ItemStack item, byte[] bytes, Throwable e) {
        String data;
        try {
            data = Base64.getEncoder().encodeToString(bytes != null ? bytes : serializer.apply(item));
        } catch (Throwable t) {
            data = "ilegible:" + item;
        }
        log.log(Level.SEVERE, "PENDIENTE PERDIDO jugador=" + id + " item(base64)=" + data, e);
    }

    /**
     * Hilo principal. Solo en WAITING_MATERIALS o READY: devuelve todo lo depositado (un WITHDRAW por material) y
     * la obra vuelve a WAITING_MATERIALS. true si todos los apuntes salieron bien. Los ítems van al dueño de la
     * obra (conectado, o a pendientes), nunca a quien pulsa.
     */
    public CompletableFuture<Boolean> withdraw(Player p, Project project) {
        if (project.busy() || project.state() != ProjectState.WAITING_MATERIALS
                && project.state() != ProjectState.READY) {
            return CompletableFuture.completedFuture(false);
        }
        Map<String, Integer> amounts = new LinkedHashMap<>();
        project.materials().forEach((key, row) -> amounts.put(key, Math.max(0, row.deposited() - row.consumed())));
        return settle(p, project, Movement.Kind.WITHDRAW, amounts, (anyOk, allOk) -> {
            if (anyOk && project.state() == ProjectState.READY) {
                project.setState(ProjectState.WAITING_MATERIALS);
                save(project);
            }
        });
    }

    /**
     * Hilo principal. Cualquier estado salvo COMPLETED y CANCELLED: todos los REFUND de {@code refundPercent} % de
     * lo no gastado y el paso a CANCELLED entran en una sola transacción. Si falla no cambia nada (ni base de datos,
     * ni memoria, ni ítems) y se puede reintentar con el mismo resultado. Lo devuelto va al dueño de la obra
     * (conectado, o a pendientes), nunca a quien pulsa. El dinero: {@link DepositMath#moneyRefund} de lo cobrado,
     * como MONEY_REFUND con su fila de {@code pending_money} en la misma transacción; luego se intenta pagar.
     */
    public CompletableFuture<Boolean> cancel(Player p, Project project, int refundPercent) {
        if (project.busy() || project.state() == ProjectState.COMPLETED
                || project.state() == ProjectState.CANCELLED) {
            return CompletableFuture.completedFuture(false);
        }
        UUID id = p.getUniqueId();
        List<Movement> refunds = new ArrayList<>();
        DepositMath.refund(project.materials(), refundPercent).forEach((key, n) -> {
            if (n > 0) {
                refunds.add(new Movement(UUID.randomUUID(), project.id(), id, Movement.Kind.REFUND, key, n));
            }
        });
        long cash = DepositMath.moneyRefund(project.moneyCents(), project.total(), project.cursor(), refundPercent);
        // El apunte de dinero nombra al dueño: es quien lo recibe (los REFUND de ítems apuntan a quien cancela).
        if (cash > 0) {
            refunds.add(new Movement(UUID.randomUUID(), project.id(), project.owner(), Movement.Kind.MONEY_REFUND,
                    Movement.MONEY_KEY, (int) cash));
        }
        project.setBusy(true);
        try {
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            repo.cancel(project.id(), refunds).exceptionally(e -> {
                log.log(Level.SEVERE, "Falló la cancelación de " + project.id() + " por " + id, e);
                return false;
            }).whenComplete((ok, e) -> main.accept(() -> {
                try {
                    if (ok) {
                        // Lo devuelto es siempre del dueño de la obra, no de quien pulsa (puede ser el de la mesa).
                        UUID to = project.owner();
                        Player now = online.apply(to);
                        // Primero fuera de la memoria: ya está CANCELLED en la base de datos pase lo que pase luego.
                        project.setState(ProjectState.CANCELLED);
                        finisher.accept(project);
                        List<ItemStack> items = new ArrayList<>();
                        refunds.stream().filter(m -> m.kind() == Movement.Kind.REFUND)
                                .forEach(m -> safeCredit(now, to, project, m.materialKey(), m.amount(), items));
                        hand(to, now, items);
                        if (now != null) {
                            now.saveData();
                        }
                        if (cash > 0 && money != null) {
                            try {
                                money.payPending(to); // sin Vault o si falla, sigue en pending_money
                            } catch (RuntimeException ex) {
                                log.log(Level.WARNING, "Dinero de " + project.id() + " sin pagar aún (pendiente)", ex);
                            }
                        }
                    }
                    result.complete(ok);
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Cancelación de " + project.id() + " con error", t);
                    result.complete(false);
                } finally {
                    project.setBusy(false);
                }
            }));
            return result;
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Fallo al cancelar " + project.id(), t);
            project.setBusy(false);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * {@link #credit} sin que un proveedor que lanza pare a los demás materiales: lo apuntado en el libro ya salió, así
     * que el log lleva lo necesario para que un admin lo reponga.
     */
    private void safeCredit(Player now, UUID id, Project project, String key, int n, List<ItemStack> items) {
        try {
            credit(now, id, project, key, n, items);
        } catch (Throwable t) {
            log.log(Level.SEVERE, "PENDIENTE PERDIDO jugador=" + id + " material=" + key + " cantidad=" + n
                    + " obra=" + project.id() + " (el proveedor no pudo crear el ítem)", t);
        }
    }

    /** Memoria al día y, si hay proveedor, ítems por {@code n} unidades; sin proveedor solo queda el aviso. */
    private void credit(Player now, UUID id, Project project, String key, int n, List<ItemStack> items) {
        DepositService.adjust(project, key, -n);
        Optional<ItemStack> sample = materials.sample(key);
        if (sample.isEmpty()) {
            log.warning("Sin proveedor para " + key + ": " + n + " apuntadas en el libro de " + project.id()
                    + " sin ítem para " + id + " (el proveedor de ese tipo ya no está registrado).");
            if (now != null) {
                message.accept(now, "deposit.provider-missing");
            }
            return;
        }
        split(sample.get(), n, items);
    }

    /** Un fallo al entregar no deshace lo apuntado: el libro ya lo tiene. */
    private void hand(UUID id, Player now, List<ItemStack> items) {
        try {
            if (!items.isEmpty()) {
                if (now == null) {
                    givePending(id, items);
                } else {
                    give(now, items);
                }
            }
        } catch (Throwable t) {
            log.log(Level.SEVERE, "No se pudieron entregar " + items.size() + " pilas a " + id, t);
        }
    }

    /** Guarda el estado de la obra; false o fallo solo se avisa: la memoria ya manda. */
    private void save(Project project) {
        repo.saveProgress(project.id(), project.state(), project.cursor(), project.placed(), project.skipped(),
                project.error()).whenComplete((ok, e) -> {
            if (e != null) {
                log.log(Level.WARNING, "No se pudo guardar el estado " + project.state() + " de " + project.id(), e);
            } else if (!ok) {
                log.warning("El estado " + project.state() + " de " + project.id()
                        + " no se guardó (obra inexistente o ya final).");
            }
        });
    }

    /**
     * Escribe un movimiento por material con cantidad positiva; solo los confirmados bajan la memoria y entregan
     * ítems. {@code after(anyOk, allOk)} corre en el hilo principal antes de soltar el bloqueo de la obra.
     */
    private CompletableFuture<Boolean> settle(Player p, Project project, Movement.Kind kind,
                                              Map<String, Integer> amounts, BiConsumer<Boolean, Boolean> after) {
        UUID id = p.getUniqueId();
        project.setBusy(true);
        try {
            Map<String, CompletableFuture<Boolean>> writes = new LinkedHashMap<>();
            amounts.forEach((key, n) -> {
                if (n > 0) {
                    writes.put(key, repo.applyMovement(new Movement(UUID.randomUUID(), project.id(), id, kind, key, n))
                            .exceptionally(e -> {
                                log.log(Level.SEVERE, "Falló el apunte " + kind + " de " + id + " en " + project.id(), e);
                                return false;
                            }));
                }
            });
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            CompletableFuture.allOf(writes.values().toArray(new CompletableFuture[0])).whenComplete((v, e) ->
                    main.accept(() -> {
                        try {
                            result.complete(payOut(p, project, amounts, writes, after));
                        } catch (Throwable t) {
                            log.log(Level.SEVERE, kind + " de " + id + " en " + project.id() + " con error", t);
                            result.complete(false);
                        } finally {
                            project.setBusy(false);
                        }
                    }));
            return result;
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Fallo al apuntar " + kind + " de " + id, t);
            project.setBusy(false);
            return CompletableFuture.completedFuture(false);
        }
    }

    private boolean payOut(Player p, Project project, Map<String, Integer> amounts,
                           Map<String, CompletableFuture<Boolean>> writes, BiConsumer<Boolean, Boolean> after) {
        // Se entrega al dueño de la obra (no a quien pulsa); pudo reconectar durante la escritura: manda la de ahora.
        UUID id = project.owner();
        Player now = online.apply(id);
        List<ItemStack> items = new ArrayList<>();
        boolean any = false;
        boolean all = true;
        for (Map.Entry<String, CompletableFuture<Boolean>> w : writes.entrySet()) {
            if (!w.getValue().join()) {
                all = false;
                continue;
            }
            any = true;
            safeCredit(now, id, project, w.getKey(), amounts.get(w.getKey()), items);
        }
        hand(id, now, items);
        if (any && now != null) {
            now.saveData();
        }
        after.accept(any, all);
        return all;
    }

    /** Reparte {@code amount} en pilas de como mucho getMaxStackSize. */
    private static void split(ItemStack sample, int amount, List<ItemStack> out) {
        int max = Math.max(1, sample.getMaxStackSize());
        while (amount > 0) {
            ItemStack part = sample.clone();
            int n = Math.min(max, amount);
            part.setAmount(n);
            out.add(part);
            amount -= n;
        }
    }

    /**
     * Hilo principal. Entrega los pendientes del jugador: cada fila se reclama en la base de datos y solo si ese
     * reclamo es el primero se entrega su ítem. Devuelve cuántas filas entregó; 0 si ya hay un recoger en curso.
     */
    public CompletableFuture<Integer> claimAll(Player p) {
        UUID id = p.getUniqueId();
        if (!claiming.add(id)) {
            return CompletableFuture.completedFuture(0);
        }
        CompletableFuture<Integer> result = new CompletableFuture<>();
        try {
            repo.pending(id).whenComplete((rows, e) -> main.accept(() -> {
                try {
                    if (e != null) {
                        log.log(Level.SEVERE, "No se pudieron leer los pendientes de " + id, e);
                        result.complete(0);
                    } else {
                        claim(id, rows, result);
                        return;
                    }
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Recoger pendientes de " + id + " con error", t);
                    result.complete(0);
                }
                claiming.remove(id);
            }));
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Fallo al leer los pendientes de " + id, t);
            claiming.remove(id);
            return CompletableFuture.completedFuture(0);
        }
        return result;
    }

    /** Hilo principal: reclama en la base de datos las filas legibles y entrega las que se reclamaron. */
    private void claim(UUID id, List<PendingRow> rows, CompletableFuture<Integer> result) {
        Map<PendingRow, ItemStack> readable = new LinkedHashMap<>();
        for (PendingRow row : rows) {
            try {
                readable.put(row, deserializer.apply(row.item()));
            } catch (Throwable t) {
                // Sin reclamar: la fila sigue pendiente en vez de perderse.
                log.log(Level.SEVERE, "Pendiente " + row.id() + " de " + id + " ilegible: se deja sin reclamar", t);
            }
        }
        Map<PendingRow, CompletableFuture<Boolean>> claims = new LinkedHashMap<>();
        readable.keySet().forEach(row -> {
            CompletableFuture<Boolean> f;
            try {
                f = repo.claim(row.id());
            } catch (Throwable t) {
                f = CompletableFuture.failedFuture(t);
            }
            claims.put(row, f.exceptionally(e -> {
                log.log(Level.SEVERE, "Falló el reclamo del pendiente " + row.id() + " de " + id, e);
                return false;
            }));
        });
        CompletableFuture.allOf(claims.values().toArray(new CompletableFuture[0])).whenComplete((v, e) ->
                main.accept(() -> {
                    try {
                        List<ItemStack> won = new ArrayList<>();
                        claims.forEach((row, f) -> {
                            if (f.join()) {
                                won.add(readable.get(row));
                            }
                        });
                        Player now = online.apply(id);
                        if (!won.isEmpty()) {
                            if (now == null) {
                                givePending(id, won);
                            } else {
                                give(now, won);
                                now.saveData();
                            }
                        }
                        result.complete(won.size());
                    } catch (Throwable t) {
                        log.log(Level.SEVERE, "Entrega de pendientes de " + id + " con error", t);
                        result.complete(0);
                    } finally {
                        claiming.remove(id);
                    }
                }));
    }
}
