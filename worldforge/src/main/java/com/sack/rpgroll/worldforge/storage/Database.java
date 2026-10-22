package com.sack.rpgroll.worldforge.storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The operational database ({@code worldforge.db}). One connection, owned by one thread:
 * every query goes through {@link #submit} and runs on that thread, never on a tick
 * thread and never two at once. The driver is the {@code sqlite-jdbc} Paper and Folia ship
 * (FEASIBILITY §1).
 */
public final class Database implements AutoCloseable {

    @FunctionalInterface
    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    /** Numbered migrations; index + 1 = schema version. Never edit one that shipped: add a new one. */
    static final List<String[]> MIGRATIONS = List.<String[]>of(new String[] {
            """
            CREATE TABLE managed_worlds(
              name TEXT PRIMARY KEY, profile_id TEXT NOT NULL, profile_hash TEXT NOT NULL,
              status TEXT NOT NULL, created_at INTEGER NOT NULL, created_by TEXT NOT NULL)""",
            """
            CREATE TABLE profile_versions(
              profile_id TEXT NOT NULL, hash TEXT NOT NULL, saved_at INTEGER NOT NULL, saved_by TEXT NOT NULL,
              PRIMARY KEY(profile_id, hash))""",
            """
            CREATE TABLE dimensions(
              id TEXT PRIMARY KEY, profile_id TEXT NOT NULL, state TEXT NOT NULL, message TEXT,
              updated_at INTEGER NOT NULL)""",
            """
            CREATE TABLE jobs(
              id TEXT PRIMARY KEY, type TEXT NOT NULL, world TEXT NOT NULL, dimension_dir TEXT NOT NULL,
              state TEXT NOT NULL, token TEXT NOT NULL, plan TEXT NOT NULL, total INTEGER NOT NULL,
              done INTEGER NOT NULL, failed INTEGER NOT NULL, backup_id TEXT, message TEXT,
              created_by TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)""",
            "CREATE INDEX jobs_state ON jobs(state)",
            "CREATE INDEX jobs_world ON jobs(world)",
            """
            CREATE TABLE backups(
              id TEXT PRIMARY KEY, job_id TEXT, world TEXT NOT NULL, path TEXT NOT NULL, files INTEGER NOT NULL,
              bytes INTEGER NOT NULL, manifest_sha256 TEXT NOT NULL, verified INTEGER NOT NULL,
              created_at INTEGER NOT NULL)""",
            """
            CREATE TABLE history(
              id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, actor TEXT NOT NULL,
              action TEXT NOT NULL, target TEXT, detail TEXT)""",
            "CREATE INDEX history_ts ON history(ts)",
    }, new String[] {
            // 2: what a datapack dimension was made of (phase 5).
            "ALTER TABLE dimensions ADD COLUMN dimension_type TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE dimensions ADD COLUMN generator TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE dimensions ADD COLUMN created_by TEXT NOT NULL DEFAULT ''",
    });

    private final ExecutorService executor;
    private final Connection connection;

    private Database(ExecutorService executor, Connection connection) {
        this.executor = executor;
        this.connection = connection;
    }

    /**
     * Opens (creating if needed), checks integrity and migrates. Blocks: call it off the
     * tick thread, or in {@code onLoad} before the server ticks.
     *
     * @throws SQLException with a readable message if the file is not a usable database
     */
    public static Database open(Path file) throws SQLException {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "WorldForge-SQLite");
            t.setDaemon(true);
            return t;
        });
        try {
            Connection c = executor.submit(() -> connect(file)).get();
            return new Database(executor, c);
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new SQLException("Interrumpido al abrir " + file, e);
        } catch (ExecutionException e) {
            executor.shutdownNow();
            if (e.getCause() instanceof SQLException sql) {
                throw sql;
            }
            throw new SQLException("No se pudo abrir " + file + ": " + e.getCause(), e.getCause());
        }
    }

    private static Connection connect(Path file) throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA busy_timeout = 5000");
            try (ResultSet rs = s.executeQuery("PRAGMA integrity_check")) {
                String result = rs.next() ? rs.getString(1) : "sin respuesta";
                if (!"ok".equalsIgnoreCase(result)) {
                    c.close();
                    throw new SQLException("La base de datos " + file.getFileName() + " está dañada (" + result + ").");
                }
            }
            // Migrate (which refuses newer schemas) before WAL, which rewrites the file header.
            migrate(c);
            s.execute("PRAGMA journal_mode = WAL");
            s.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            c.close();
            if (e.getMessage() != null && e.getMessage().contains("not a database")) {
                throw new SQLException("«" + file.getFileName() + "» no es una base de datos SQLite (¿archivo dañado o sustituido?).", e);
            }
            throw e;
        }
        return c;
    }

    static void migrate(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS schema_version(version INTEGER NOT NULL)");
        }
        int current = version(c);
        if (current > MIGRATIONS.size()) {
            throw new SQLException("La base de datos es de un WorldForge más nuevo (esquema " + current
                    + ", este conoce hasta " + MIGRATIONS.size() + "). No se toca.");
        }
        for (int v = current; v < MIGRATIONS.size(); v++) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                for (String sql : MIGRATIONS.get(v)) {
                    s.execute(sql);
                }
                s.execute("DELETE FROM schema_version");
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO schema_version(version) VALUES (?)")) {
                    ps.setInt(1, v + 1);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw new SQLException("Falló la migración " + (v + 1) + ": " + e.getMessage(), e);
            } finally {
                c.setAutoCommit(auto);
            }
        }
    }

    static int version(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT MAX(version) FROM schema_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public <T> CompletableFuture<T> submit(Work<T> work) {
        CompletableFuture<T> f = new CompletableFuture<>();
        try {
            executor.execute(() -> {
                try {
                    f.complete(work.run(connection));
                } catch (Throwable t) {
                    f.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            f.completeExceptionally(t);
        }
        return f;
    }

    /** Runs a transaction on the database thread: all statements or none. */
    public <T> CompletableFuture<T> transaction(Work<T> work) {
        return submit(c -> {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(auto);
            }
        });
    }

    /** Waits for {@link #submit}. Only off the tick thread (commands use {@link #submit}). */
    public <T> T call(Work<T> work) throws SQLException {
        try {
            return submit(work).get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrumpido", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof SQLException sql) {
                throw sql;
            }
            throw new SQLException(e.getCause());
        } catch (java.util.concurrent.TimeoutException e) {
            throw new SQLException("La base de datos no respondió en 30 s", e);
        }
    }

    public int schemaVersion() throws SQLException {
        return call(Database::version);
    }

    @Override
    public void close() {
        executor.execute(() -> {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // closing anyway
            }
        });
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
