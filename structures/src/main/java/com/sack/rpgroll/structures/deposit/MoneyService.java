package com.sack.rpgroll.structures.deposit;

import com.sack.rpgroll.structures.storage.PendingMoneyRow;
import com.sack.rpgroll.structures.storage.ProjectRepository;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dinero de las obras ({@code money:}) por Vault, en céntimos. La economía de Vault no es segura entre hilos: todo
 * lo que la toca corre en el hilo principal. Lo que no se puede pagar queda en {@code pending_money} y se paga
 * después ({@link #payPending}); nunca se pierde sin dejar fila o SEVERE en el log.
 */
public final class MoneyService {

    public enum ChargeResult { OK, NOT_ENOUGH, UNAVAILABLE, ERROR }

    private final ProjectRepository repo;
    private final BooleanSupplier active;
    private final Supplier<Optional<Economy>> economy;
    private final Function<UUID, OfflinePlayer> offline;
    private final Consumer<Runnable> main;
    private final Logger log;
    /** Filas ya pagadas (o pagándose) en esta sesión: nunca se pagan dos veces aunque la marca no llegue a la base. */
    private final Set<Long> paid = ConcurrentHashMap.newKeySet();

    /**
     * @param active  la integración de Vault activa ({@code Integrations.active(VAULT)})
     * @param economy la economía de Vault ahora ({@code VaultEconomy::get})
     * @param offline jugador por UUID, conectado o no ({@code Bukkit::getOfflinePlayer})
     */
    public MoneyService(ProjectRepository repo, BooleanSupplier active, Supplier<Optional<Economy>> economy,
                        Function<UUID, OfflinePlayer> offline, Consumer<Runnable> main, Logger log) {
        this.repo = repo;
        this.active = active;
        this.economy = economy;
        this.offline = offline;
        this.main = main;
        this.log = log;
    }

    /** El dinero del YAML en céntimos. */
    public static long cents(double money) {
        return Math.round(money * 100);
    }

    public static String format(long cents) {
        return String.format(Locale.ROOT, "%.2f", cents / 100.0);
    }

    public boolean available() {
        return eco() != null;
    }

    private Economy eco() {
        return active.getAsBoolean() ? economy.get().orElse(null) : null;
    }

    /** Hilo principal. Cobra {@code cents} enteros o nada. */
    public ChargeResult charge(Player p, long cents) {
        if (cents <= 0) {
            return ChargeResult.OK;
        }
        Economy e = eco();
        if (e == null) {
            return ChargeResult.UNAVAILABLE;
        }
        double amount = cents / 100.0;
        try {
            if (!e.has(p, amount)) {
                return ChargeResult.NOT_ENOUGH;
            }
            if (e.withdrawPlayer(p, amount).transactionSuccess()) {
                return ChargeResult.OK;
            }
            log.warning("La economía rechazó cobrar " + format(cents) + " a " + p.getUniqueId());
            return ChargeResult.ERROR;
        } catch (RuntimeException ex) {
            // No se sabe si llegó a cobrar: el log lleva lo necesario para que un admin lo revise.
            log.log(Level.SEVERE, "Cobro de " + format(cents) + " a " + p.getUniqueId()
                    + " con error; revisar su saldo", ex);
            return ChargeResult.ERROR;
        }
    }

    /**
     * Hilo principal. Devuelve un cobro ya hecho (el alta de la obra falló). Si no se puede pagar ahora, SEVERE y
     * fila en {@code pending_money} para pagarlo luego.
     */
    public void refund(UUID owner, long cents, String movementId) {
        if (cents <= 0 || deposit(owner, cents)) {
            return;
        }
        log.severe("DINERO PERDIDO " + owner + " " + format(cents) + " (cobro " + movementId
                + "); queda en pending_money para pagarlo al entrar o al arrancar con Vault");
        try {
            repo.addPendingMoney(owner, cents).exceptionally(e -> {
                log.log(Level.SEVERE, "DINERO PERDIDO " + owner + " " + format(cents) + " (cobro " + movementId
                        + "): tampoco se pudo guardar en pending_money", e);
                return null;
            });
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "DINERO PERDIDO " + owner + " " + format(cents) + " (cobro " + movementId
                    + "): tampoco se pudo guardar en pending_money", e);
        }
    }

    /** Hilo principal. true solo si la economía confirmó el ingreso. */
    private boolean deposit(UUID player, long cents) {
        try {
            Economy e = eco();
            return e != null && e.depositPlayer(offline.apply(player), cents / 100.0).transactionSuccess();
        } catch (RuntimeException ex) {
            log.log(Level.WARNING, "Ingreso de " + format(cents) + " a " + player + " con error", ex);
            return false;
        }
    }

    /**
     * Paga el dinero pendiente del jugador (conectado o no). Cada fila: primero se ingresa en el hilo principal y
     * luego se marca {@code paid_at}. Devuelve cuántas filas pagó. Sin Vault no hace nada (todo sigue pendiente).
     */
    public CompletableFuture<Integer> payPending(UUID player) {
        if (!available()) {
            return CompletableFuture.completedFuture(0);
        }
        CompletableFuture<Integer> result = new CompletableFuture<>();
        try {
            repo.pendingMoney(player).whenComplete((rows, err) -> main.accept(() -> {
                try {
                    if (err != null) {
                        log.log(Level.SEVERE, "No se pudo leer el dinero pendiente de " + player, err);
                        result.complete(0);
                        return;
                    }
                    int n = 0;
                    for (PendingMoneyRow row : rows) {
                        if (pay(row)) {
                            n++;
                        }
                    }
                    result.complete(n);
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Pago del dinero pendiente de " + player + " con error", t);
                    result.complete(0);
                }
            }));
        } catch (RuntimeException e) {
            log.log(Level.SEVERE, "No se pudo leer el dinero pendiente de " + player, e);
            return CompletableFuture.completedFuture(0);
        }
        return result;
    }

    /**
     * Ingresar antes de marcar: si la marca falla, la fila sigue en la guarda (no se repite en esta sesión) y solo
     * un reinicio la pagaría otra vez (SEVERE); al revés, un ingreso fallido tras marcar perdería el dinero.
     */
    private boolean pay(PendingMoneyRow row) {
        if (!paid.add(row.id())) {
            return false;
        }
        if (!deposit(row.player(), row.amountCents())) {
            paid.remove(row.id()); // sigue pendiente; se reintenta
            return false;
        }
        CompletableFuture<Boolean> mark;
        try {
            mark = repo.markMoneyPaid(row.id());
        } catch (RuntimeException e) {
            mark = CompletableFuture.failedFuture(e);
        }
        mark.whenComplete((ok, e) -> {
            if (e != null || !Boolean.TRUE.equals(ok)) {
                log.log(Level.SEVERE, "Dinero pendiente " + row.id() + " (" + format(row.amountCents()) + " a "
                        + row.player() + ") pagado pero sin marcar: no se repite hasta reiniciar; marcar paid_at"
                        + " a mano para que no se pague otra vez", e);
            }
        });
        return true;
    }
}
