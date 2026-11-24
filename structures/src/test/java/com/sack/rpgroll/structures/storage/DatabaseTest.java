package com.sack.rpgroll.structures.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {
    @TempDir Path dir;

    Database open() throws SQLException {
        return Database.open(Dialect.SQLITE, "jdbc:sqlite:" + dir.resolve("data.db"), "", "", Logger.getAnonymousLogger());
    }

    @Test void submitAfterCloseIsAFailedFutureNotAThrow() throws Exception {
        Database db = open();
        db.close();
        CompletableFuture<Integer> f = assertDoesNotThrow(() -> db.submit(c -> 1));
        CompletionException e = assertThrows(CompletionException.class, f::join);
        assertInstanceOf(SQLException.class, e.getCause());
    }

    @Test void closeWaitsAtMostFiveSecondsPerPhase() throws Exception {
        Database db = open();
        try {
            assertEquals(5_000, db.closeTimeoutMillis);
        } finally {
            db.close();
        }
    }

    @Test void mysqlUrlCarriesASocketTimeout() {
        // Sin socketTimeout una consulta a un MySQL que dejó de responder cuelga el hilo de la base para siempre.
        assertEquals("jdbc:mariadb://db.local:3307/estructuras?socketTimeout=30000",
                Database.mysqlUrl("db.local", 3307, "estructuras"));
    }

    @Test void closeNeverClosesTheConnectionUnderARunningTask() throws Exception {
        Database db = open();
        db.closeTimeoutMillis = 50;
        Connection conn = db.submit(c -> c).join();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean closedUnderTask = new AtomicBoolean(true);
        CompletableFuture<Void> task = db.submit(c -> {
            started.countDown();
            // No hace caso de la interrupción, como un driver colgado.
            while (release.getCount() > 0) {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                    // sigue esperando
                }
            }
            closedUnderTask.set(c.isClosed());
            return null;
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));

        long t0 = System.nanoTime();
        db.close(); // dos plazos sin terminar: vuelve sin cerrar la conexión
        assertTrue(System.nanoTime() - t0 < TimeUnit.SECONDS.toNanos(3), "close() no debe esperar a la tarea");
        assertFalse(conn.isClosed(), "la conexión sigue abierta mientras la tarea corre");

        release.countDown();
        task.get(5, TimeUnit.SECONDS);
        assertFalse(closedUnderTask.get(), "la tarea terminó con la conexión abierta");
        long end = System.currentTimeMillis() + 5000;
        while (!conn.isClosed() && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        assertTrue(conn.isClosed(), "la conexión se cierra cuando la tarea termina");
    }

    @Test void closeInterruptsATaskThatSleepsPastTheTimeout() throws Exception {
        Database db = open();
        db.closeTimeoutMillis = 50;
        Connection conn = db.submit(c -> c).join();
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        db.submit(c -> {
            started.countDown();
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                interrupted.set(true);
            }
            return null;
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));

        long t0 = System.nanoTime();
        db.close();
        long took = System.nanoTime() - t0;

        assertTrue(took < TimeUnit.MILLISECONDS.toNanos(2 * 50 + 2000), "tardó " + took / 1_000_000 + " ms");
        assertTrue(interrupted.get(), "la tarea se interrumpió");
        assertTrue(conn.isClosed(), "con la tarea ya fuera, la conexión se cierra en el momento");
    }
}
