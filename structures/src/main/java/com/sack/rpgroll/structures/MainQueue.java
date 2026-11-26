package com.sack.rpgroll.structures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Vuelta al hilo principal propia, en vez de {@code runTask}: dentro de {@code onDisable} Paper ya no acepta tareas
 * (el plugin no está habilitado) y cancela las que había en cola, así que una entrega cuyo apunte ya está en la base
 * de datos se perdería. Una tarea repetida cada tick llama a {@link #drain}; al apagar, {@link #drainAtShutdown}.
 */
public final class MainQueue implements Executor {

    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final Logger log;

    public MainQueue(Logger log) {
        this.log = log;
    }

    /** Desde cualquier hilo. */
    @Override
    public void execute(Runnable r) {
        tasks.add(r);
    }

    /**
     * Hilo principal: corre lo que había al empezar (lo que se encole mientras, en la vuelta siguiente). Una tarea
     * que lanza no para a las demás. Devuelve cuántas corrió.
     */
    public int drain() {
        int n = 0;
        for (int left = tasks.size(); left > 0; left--) {
            Runnable r = tasks.poll();
            if (r == null) {
                break;
            }
            n++;
            try {
                r.run();
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Tarea del hilo principal con error", t);
            }
        }
        return n;
    }

    /**
     * Hilo principal, al apagar: espera a que la base de datos termine lo pendiente ({@code barrier}) y vacía la
     * cola en el sitio; otra vez mientras haya algo, porque una entrega puede escribir pendientes y volver aquí.
     * Como mucho {@code rounds} vueltas. true si al final no quedó nada.
     */
    public boolean drainAtShutdown(Supplier<CompletableFuture<?>> barrier, long timeoutMs, int rounds) {
        for (int i = 0; i < rounds; i++) {
            try {
                barrier.get().get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.warning("La base de datos no terminó en " + timeoutMs + " ms al apagar; se entrega lo que hay.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | RuntimeException e) {
                log.log(Level.WARNING, "Barrera de la base de datos al apagar", e);
            }
            if (drain() == 0 && tasks.isEmpty()) {
                return true;
            }
        }
        if (!tasks.isEmpty()) {
            log.severe(tasks.size() + " tarea(s) del hilo principal sin correr al apagar tras " + rounds
                    + " vueltas; lo ya apuntado en el libro sigue ahí (revisar el log).");
        }
        return tasks.isEmpty();
    }
}
