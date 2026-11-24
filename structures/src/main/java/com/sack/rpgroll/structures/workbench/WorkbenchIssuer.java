package com.sack.rpgroll.structures.workbench;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Emite mesas nuevas: id aleatorio, serie de la base y firma. Dos caminos:
 * <ul>
 *   <li>{@link #issue}: asíncrono, para {@code give}; el futuro termina en el hilo principal.</li>
 *   <li>{@link #issueNow}: síncrono, para {@code /dar} y la receta (sus APIs no esperan). Gasta series de un
 *       depósito en memoria que se rellena en segundo plano; si está vacío devuelve vacío y no bloquea.</li>
 * </ul>
 * Las series del depósito que no se usen al apagar quedan como huecos en la numeración; no se reutilizan.
 */
public final class WorkbenchIssuer {

    /**
     * Series que se mantienen listas para {@link #issueNow}: un {@code /dar mesa:<nivel> 64} seguido no lo agota.
     * Las que queden al apagar son huecos en la numeración (como mucho 64 por arranque).
     */
    static final int POOL_TARGET = 64;

    private final Supplier<CompletableFuture<Long>> serials;
    private final SerialSigner signer;
    private final Executor main;
    private final Logger log;
    private final ConcurrentLinkedQueue<Long> pool = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pending = new AtomicInteger();

    public WorkbenchIssuer(Supplier<CompletableFuture<Long>> serials, SerialSigner signer, Executor main, Logger log) {
        this.serials = serials;
        this.signer = signer;
        this.main = main;
        this.log = log;
    }

    /** Pide a la base las series que faltan para llenar el depósito. */
    public void refill() {
        int missing = POOL_TARGET - pool.size() - pending.get();
        for (int i = 0; i < missing; i++) {
            pending.incrementAndGet();
            serials.get().whenCompleteAsync((serial, err) -> {
                pending.decrementAndGet();
                if (err != null) {
                    log.warning("No se pudo reservar un número de serie: " + err);
                } else {
                    pool.add(serial);
                }
            }, main);
        }
    }

    /** Una mesa ya, o vacío (con aviso) si el depósito de series está vacío. */
    public Optional<WorkbenchStamp> issueNow(WorkbenchTier tier) {
        Long serial = pool.poll();
        refill();
        if (serial == null) {
            log.warning("Sin series de mesa reservadas todavía; no se pudo emitir la mesa. Reintenta en un momento.");
            return Optional.empty();
        }
        return Optional.of(stamp(tier, serial));
    }

    /** {@code amount} mesas nuevas, cada una con su id y su serie; termina en el hilo principal. */
    public CompletableFuture<List<WorkbenchStamp>> issue(WorkbenchTier tier, int amount) {
        List<CompletableFuture<Long>> reserved = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            reserved.add(serials.get());
        }
        // handleAsync (no thenApplyAsync) para que también el fallo llegue en el hilo principal.
        return CompletableFuture.allOf(reserved.toArray(CompletableFuture[]::new)).handleAsync((v, err) -> {
            if (err != null) {
                throw new CompletionException(err);
            }
            return reserved.stream().map(f -> stamp(tier, f.join())).toList();
        }, main);
    }

    private WorkbenchStamp stamp(WorkbenchTier tier, long serial) {
        UUID id = UUID.randomUUID();
        return new WorkbenchStamp(id, tier, serial, signer.sign(id, tier, serial));
    }
}
