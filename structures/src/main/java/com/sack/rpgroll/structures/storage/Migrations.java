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

/**
 * Migraciones numeradas. Una que ya salió no se edita: se añade otra.
 * <p>
 * Excepción: v1 se cambió el 2026-11-23, antes de publicarse (solo la usó el banco de QA). Las bases de ese banco
 * tienen la unicidad de la posición en el índice {@code workbenches_location}; las nuevas, en un UNIQUE dentro de la
 * tabla (sin nombre fijo). Una v2 no debe nombrar {@code workbenches_location} (ni crearlo ni borrarlo): en unas bases
 * existe y en otras no.
 */
final class Migrations {

    static final int LATEST = 6;

    private Migrations() {
    }

    /**
     * Cada sentencia se puede repetir: el DDL de MySQL no es transaccional, y una migración que falló a medias
     * deja tablas creadas sin la versión. La unicidad de la posición va dentro de la tabla porque MySQL no tiene
     * {@code CREATE INDEX IF NOT EXISTS}. El camino de MySQL no tiene prueba contra un servidor real.
     */
    static List<String> v1(Dialect d) {
        String t = d.shortText();
        return List.of(
                "CREATE TABLE IF NOT EXISTS workbenches(id " + t + " PRIMARY KEY, owner " + t + " NOT NULL, world "
                        + t + " NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, tier " + t
                        + " NOT NULL, serial BIGINT NOT NULL UNIQUE, orphan INTEGER NOT NULL DEFAULT 0,"
                        + " created_at BIGINT NOT NULL, UNIQUE(world, x, y, z))",
                "CREATE TABLE IF NOT EXISTS workbench_access(workbench_id " + t + " NOT NULL, player " + t
                        + " NOT NULL, PRIMARY KEY(workbench_id, player),"
                        + " FOREIGN KEY(workbench_id) REFERENCES workbenches(id) ON DELETE CASCADE)",
                "CREATE TABLE IF NOT EXISTS serials(next BIGINT NOT NULL)",
                "INSERT INTO serials(next) SELECT 1 FROM (SELECT 1 AS one) AS seed"
                        + " WHERE NOT EXISTS (SELECT 1 FROM serials)");
    }

    /**
     * Obras. {@code cursor} va entre comillas inversas porque en MySQL es palabra reservada. {@code size_*} es la
     * medida ya girada, para calcular la caja de una obra sin leer la estructura.
     */
    static List<String> v2(Dialect d) {
        String t = d.shortText();
        return List.of(
                "CREATE TABLE IF NOT EXISTS projects(id " + t + " PRIMARY KEY, bench_id " + t + " NOT NULL,"
                        + " structure_id " + t + " NOT NULL, owner " + t + " NOT NULL, state " + t + " NOT NULL,"
                        + " world " + t + " NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL,"
                        + " rotation " + t + " NOT NULL, `cursor` INTEGER NOT NULL DEFAULT 0,"
                        + " placed INTEGER NOT NULL DEFAULT 0, skipped INTEGER NOT NULL DEFAULT 0,"
                        + " total INTEGER NOT NULL DEFAULT 0, error TEXT, size_x INTEGER NOT NULL DEFAULT 1,"
                        + " size_y INTEGER NOT NULL DEFAULT 1, size_z INTEGER NOT NULL DEFAULT 1,"
                        + " created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL)",
                "CREATE TABLE IF NOT EXISTS project_materials(project_id " + t + " NOT NULL, material_key " + t
                        + " NOT NULL, required INTEGER NOT NULL, deposited INTEGER NOT NULL DEFAULT 0,"
                        + " consumed INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(project_id, material_key))",
                "CREATE TABLE IF NOT EXISTS ledger(id " + t + " PRIMARY KEY, project_id " + t + " NOT NULL, player "
                        + t + " NOT NULL, kind " + t + " NOT NULL, material_key " + t + " NOT NULL,"
                        + " amount INTEGER NOT NULL, created_at BIGINT NOT NULL)",
                "CREATE TABLE IF NOT EXISTS pending_returns(id " + d.autoId() + ", player " + t + " NOT NULL,"
                        + " item BLOB NOT NULL, created_at BIGINT NOT NULL, claimed_at BIGINT NULL)",
                "CREATE TABLE IF NOT EXISTS builds(id " + t + " PRIMARY KEY, project_id " + t + " NOT NULL,"
                        + " structure_id " + t + " NOT NULL, owner " + t + " NOT NULL, world " + t + " NOT NULL,"
                        + " x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, rotation " + t + " NOT NULL,"
                        + " min_x INTEGER NOT NULL, min_y INTEGER NOT NULL, min_z INTEGER NOT NULL,"
                        + " max_x INTEGER NOT NULL, max_y INTEGER NOT NULL, max_z INTEGER NOT NULL,"
                        + " completed_at BIGINT NOT NULL)");
    }

    /**
     * Hash del esquemático por obra (null en las de antes: sin comprobar). Ni SQLite ni MySQL tienen
     * {@code ADD COLUMN IF NOT EXISTS}: se mira antes si la columna ya está, así repetirlo no falla.
     */
    static void v3(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT * FROM projects WHERE 1 = 0")) {
                var meta = rs.getMetaData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    if ("source_hash".equalsIgnoreCase(meta.getColumnName(i))) {
                        return;
                    }
                }
            }
            s.execute("ALTER TABLE projects ADD COLUMN source_hash VARCHAR(64) NULL");
        }
    }

    /**
     * Dinero: lo cobrado por obra ({@code money_cents}) y lo que falta pagar ({@code pending_money}). Repetible como
     * v3: la columna se mira antes del ALTER y la tabla lleva IF NOT EXISTS.
     */
    static void v4(Connection c, Dialect d) throws SQLException {
        try (Statement s = c.createStatement()) {
            if (!hasColumn(s, "projects", "money_cents")) {
                s.execute("ALTER TABLE projects ADD COLUMN money_cents BIGINT NOT NULL DEFAULT 0");
            }
            for (String sql : v4Tables(d)) {
                s.execute(sql);
            }
        }
    }

    static List<String> v4Tables(Dialect d) {
        return List.of("CREATE TABLE IF NOT EXISTS pending_money(id " + d.autoId() + ", player " + d.shortText()
                + " NOT NULL, amount_cents BIGINT NOT NULL, created_at BIGINT NOT NULL, paid_at BIGINT NULL)");
    }

    /** Datos de los módulos por construcción (p. ej. el terreno que creó una parcela). Repetible: IF NOT EXISTS. */
    static List<String> v5(Dialect d) {
        return List.of("CREATE TABLE IF NOT EXISTS build_modules(build_id " + d.shortText() + " PRIMARY KEY,"
                + " data TEXT NOT NULL)");
    }

    /**
     * Reparar y mejorar: qué hace la obra ({@code kind}), la construcción a la que va ({@code build_id}), los bloques
     * pagados de una reparación ({@code repair_blocks}) y el hash del esquemático de cada construcción
     * ({@code builds.source_hash}; null en las de antes: sin comprobar). Repetible como v3: cada columna se mira antes
     * del ALTER.
     */
    static void v6(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            if (!hasColumn(s, "projects", "kind")) {
                s.execute("ALTER TABLE projects ADD COLUMN kind VARCHAR(16) NOT NULL DEFAULT 'BUILD'");
            }
            if (!hasColumn(s, "projects", "build_id")) {
                s.execute("ALTER TABLE projects ADD COLUMN build_id VARCHAR(64) NULL");
            }
            if (!hasColumn(s, "projects", "repair_blocks")) {
                s.execute("ALTER TABLE projects ADD COLUMN repair_blocks INTEGER NOT NULL DEFAULT 0");
            }
            if (!hasColumn(s, "builds", "source_hash")) {
                s.execute("ALTER TABLE builds ADD COLUMN source_hash VARCHAR(64) NULL");
            }
        }
    }

    private static boolean hasColumn(Statement s, String table, String column) throws SQLException {
        try (ResultSet rs = s.executeQuery("SELECT * FROM " + table + " WHERE 1 = 0")) {
            var meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                if (column.equalsIgnoreCase(meta.getColumnName(i))) {
                    return true;
                }
            }
        }
        return false;
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
            }
            if (current < 2) {
                for (String sql : v2(d)) {
                    s.execute(sql);
                }
            }
            if (current < 3) {
                v3(c);
            }
            if (current < 4) {
                v4(c, d);
            }
            if (current < 5) {
                for (String sql : v5(d)) {
                    s.execute(sql);
                }
            }
            if (current < 6) {
                v6(c);
            }
            s.execute("DELETE FROM schema_version");
            s.execute("INSERT INTO schema_version(version) VALUES (" + LATEST + ")");
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
