package com.sack.rpgroll.structures.storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Una conexión, un hilo («RPGRoll-Structures-DB»): todo el trabajo pasa por {@link #submit} y nunca
 * corre en el hilo principal ni dos a la vez.
 * <p>
 * shortcut: sin reconexión automática (si MySQL corta la conexión hay que reiniciar el plugin);
 * subir a un pool si llega a hacer falta.
 */
public final class Database implements AutoCloseable {

    private final ExecutorService executor;
    private final Connection connection;
    private final Logger log;
    /**
     * Espera de {@link #close} en cada una de sus dos fases (como mucho 10 s de apagado en total); las pruebas la
     * acortan.
     */
    long closeTimeoutMillis = 5_000;

    /** Plazo de lectura del socket de MySQL/MariaDB: sin él, una consulta a un servidor mudo cuelga el hilo. */
    static final int MYSQL_SOCKET_TIMEOUT_MS = 30_000;

    private Database(ExecutorService executor, Connection connection, Logger log) {
        this.executor = executor;
        this.connection = connection;
        this.log = log;
    }

    /** URL JDBC de MariaDB/MySQL con {@code socketTimeout}. */
    public static String mysqlUrl(String host, int port, String database) {
        return "jdbc:mariadb://" + host + ":" + port + "/" + database + "?socketTimeout=" + MYSQL_SOCKET_TIMEOUT_MS;
    }

    /** Abre, migra y devuelve la base. Bloquea: llamar al arrancar, antes de que haya jugadores. */
    public static Database open(Dialect dialect, String url, String user, String password, Logger log)
            throws SQLException {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "RPGRoll-Structures-DB");
            t.setDaemon(true);
            return t;
        });
        try {
            Connection c = executor.submit(() -> connect(dialect, url, user, password, log)).get();
            return new Database(executor, c, log);
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new SQLException("Interrumpido al abrir la base de datos", e);
        } catch (ExecutionException e) {
            executor.shutdownNow();
            if (e.getCause() instanceof SQLException sql) {
                throw sql;
            }
            throw new SQLException("No se pudo abrir la base de datos: " + e.getCause(), e.getCause());
        }
    }

    private static Connection connect(Dialect dialect, String url, String user, String password, Logger log)
            throws SQLException {
        if (dialect == Dialect.MYSQL) {
            // El driver de plugin.yml `libraries:` vive en el cargador del plugin: DriverManager no lo descubre solo.
            try {
                Class.forName("org.mariadb.jdbc.Driver", true, Database.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new SQLException("Falta el driver de MariaDB", e);
            }
        }
        Connection c = dialect == Dialect.SQLITE
                ? DriverManager.getConnection(url)
                : DriverManager.getConnection(url, user, password);
        try {
            Path backups = null;
            if (dialect == Dialect.SQLITE) {
                try (Statement s = c.createStatement()) {
                    s.execute("PRAGMA busy_timeout = 5000");
                    s.execute("PRAGMA journal_mode = WAL");
                    s.execute("PRAGMA foreign_keys = ON");
                }
                String file = url.substring("jdbc:sqlite:".length());
                backups = Path.of(file).toAbsolutePath().getParent().resolve("backups");
            }
            Migrations.run(c, dialect, backups, log);
            return c;
        } catch (SQLException | RuntimeException e) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // ya falló al abrir; se informa el error original
            }
            throw e;
        }
    }

    /** Tras {@link #close} devuelve un futuro fallido (SQLException) en vez de lanzar. */
    public <T> CompletableFuture<T> submit(SqlWork<T> work) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return work.run(connection);
                } catch (SQLException e) {
                    throw new CompletionException(e);
                }
            }, executor);
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(new SQLException("La base de datos ya está cerrada", e));
        }
    }

    /**
     * Espera las tareas pendientes; si siguen tras el plazo, las interrumpe y espera otra vez. La conexión nunca
     * se cierra con una tarea aún corriendo: si ni así termina, se cierra en segundo plano cuando acabe.
     */
    @Override
    public void close() {
        executor.shutdown();
        if (!await()) {
            log.warning("La base de datos tenía tareas sin terminar tras " + closeTimeoutMillis + " ms; se interrumpen.");
            executor.shutdownNow();
            if (!await()) {
                log.warning("Una tarea de la base de datos no responde; la conexión se cerrará cuando termine.");
                Thread t = new Thread(() -> {
                    while (!executor.isTerminated()) {
                        try {
                            executor.awaitTermination(1, TimeUnit.MINUTES);
                        } catch (InterruptedException e) {
                            return; // apagando la JVM: no se cierra sobre una tarea viva
                        }
                    }
                    closeConnection();
                }, "RPGRoll-Structures-DB-close");
                t.setDaemon(true);
                t.start();
                return;
            }
        }
        closeConnection();
    }

    private boolean await() {
        try {
            return executor.awaitTermination(closeTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return executor.isTerminated();
        }
    }

    private void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            log.log(Level.WARNING, "No se pudo cerrar la base de datos", e);
        }
    }
}
