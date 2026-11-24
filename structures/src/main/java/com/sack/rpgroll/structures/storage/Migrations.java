package com.sack.rpgroll.structures.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/** Migraciones numeradas. Una que ya salió no se edita: se añade otra. */
final class Migrations {

    static final int LATEST = 1;

    private Migrations() {
    }

    static List<String> v1(Dialect d) {
        String t = d.shortText();
        return List.of(
                "CREATE TABLE workbenches(id " + t + " PRIMARY KEY, owner " + t + " NOT NULL, world " + t
                        + " NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, tier " + t
                        + " NOT NULL, serial BIGINT NOT NULL UNIQUE, orphan INTEGER NOT NULL DEFAULT 0,"
                        + " created_at BIGINT NOT NULL)",
                "CREATE UNIQUE INDEX workbenches_location ON workbenches(world, x, y, z)",
                "CREATE TABLE workbench_access(workbench_id " + t + " NOT NULL, player " + t
                        + " NOT NULL, PRIMARY KEY(workbench_id, player),"
                        + " FOREIGN KEY(workbench_id) REFERENCES workbenches(id) ON DELETE CASCADE)",
                "CREATE TABLE serials(next BIGINT NOT NULL)",
                "INSERT INTO serials(next) VALUES (1)");
    }

    /** @param backups carpeta de copias (solo SQLite; null en MySQL) */
    static void run(Connection c, Dialect d, Path backups, Logger log) throws SQLException {
        int current = currentVersion(c, d);
        if (current >= LATEST) {
            return;
        }
        if (backups != null && hasTables(c)) {
            backup(c, backups, current);
        }
        c.setAutoCommit(false);
        try (Statement s = c.createStatement()) {
            if (current == 0) {
                s.execute("CREATE TABLE IF NOT EXISTS schema_version(version INTEGER NOT NULL)");
                for (String sql : v1(d)) {
                    s.execute(sql);
                }
                s.execute("DELETE FROM schema_version");
                s.execute("INSERT INTO schema_version(version) VALUES (" + LATEST + ")");
            }
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
        log.info("Base de datos migrada a la versión " + LATEST + ".");
    }

    static int currentVersion(Connection c, Dialect d) throws SQLException {
        String exists = d == Dialect.SQLITE
                ? "SELECT 1 FROM sqlite_master WHERE type='table' AND name='schema_version'"
                : "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE()"
                        + " AND table_name = 'schema_version'";
        try (Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery(exists)) {
                if (!rs.next()) {
                    return 0;
                }
            }
            try (ResultSet rs = s.executeQuery("SELECT MAX(version) FROM schema_version")) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static boolean hasTables(Connection c) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT 1 FROM sqlite_master WHERE type='table' LIMIT 1")) {
            return rs.next();
        }
    }

    /** VACUUM INTO da una copia consistente aunque el archivo esté en modo WAL. */
    private static void backup(Connection c, Path dir, int version) throws SQLException {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new SQLException("No se pudo crear " + dir, e);
        }
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
        Path target = dir.resolve("data-" + version + "-" + stamp + ".db");
        try (Statement s = c.createStatement()) {
            s.execute("VACUUM INTO '" + target.toString().replace("'", "''") + "'");
        }
    }
}
