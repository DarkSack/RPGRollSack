package com.sack.rpgroll.structures.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MigrationsTest {

    @TempDir Path dir;
    final Logger log = Logger.getAnonymousLogger();

    @Test void migrationsAreIdempotent() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("a.db"))) {
            Migrations.run(c, Dialect.SQLITE, null, log);
            Migrations.run(c, Dialect.SQLITE, null, log);
            assertEquals(Migrations.LATEST, Migrations.currentVersion(c, Dialect.SQLITE));
        }
    }

    @Test void halfFailedMigrationCanBeRetried() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("a.db"))) {
            // Como un MySQL que cortó a mitad: su DDL no es transaccional y deja tablas sin la versión.
            try (Statement s = c.createStatement()) {
                s.execute(Migrations.v1(Dialect.SQLITE).get(0));
                s.execute(Migrations.v1(Dialect.SQLITE).get(2));
                s.execute(Migrations.v1(Dialect.SQLITE).get(3));
            }
            Migrations.run(c, Dialect.SQLITE, null, log);
            assertEquals(Migrations.LATEST, Migrations.currentVersion(c, Dialect.SQLITE));
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM serials")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "una sola fila de series");
            }
        }
    }

    @Test void everyDdlStatementCanBeRepeated() {
        for (Dialect d : Dialect.values()) {
            List<String> all = new java.util.ArrayList<>(Migrations.v1(d));
            all.addAll(Migrations.v2(d));
            for (String sql : all) {
                assertTrue(sql.contains("IF NOT EXISTS") || sql.contains("WHERE NOT EXISTS"), d + ": " + sql);
                assertFalse(sql.contains("workbenches_location"), d + ": " + sql);
            }
        }
    }

    @Test void freshDatabaseIsAtV2() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("a.db"))) {
            Migrations.run(c, Dialect.SQLITE, null, log);
            assertEquals(Migrations.LATEST, Migrations.currentVersion(c, Dialect.SQLITE));
            assertEquals(6, Migrations.LATEST);
            try (Statement s = c.createStatement()) {
                s.executeQuery("SELECT source_hash, money_cents, kind, build_id, repair_blocks FROM projects").close();
                s.executeQuery("SELECT source_hash FROM builds").close();
            }
            for (String t : List.of("projects", "project_materials", "ledger", "pending_returns", "builds",
                    "pending_money", "build_modules")) {
                try (Statement s = c.createStatement()) {
                    s.executeQuery("SELECT * FROM " + t).close();
                }
            }
        }
    }

    @Test void migratesV1DatabaseToV2KeepingBenches() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("a.db"));
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE schema_version(version INTEGER NOT NULL)");
            for (String sql : Migrations.v1(Dialect.SQLITE)) {
                s.execute(sql);
            }
            s.execute("INSERT INTO schema_version(version) VALUES (1)");
            s.execute("INSERT INTO workbenches(id, owner, world, x, y, z, tier, serial, created_at)"
                    + " VALUES ('b','o','w',1,2,3,'CONSTRUCTOR',1,5)");
            Migrations.run(c, Dialect.SQLITE, null, log);
            assertEquals(Migrations.LATEST, Migrations.currentVersion(c, Dialect.SQLITE));
            try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM workbenches")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
            s.executeQuery("SELECT * FROM projects").close();
        }
    }

    @Test void v1DatabaseIsBackedUpBeforeMigratingToV2() throws Exception {
        String url = "jdbc:sqlite:" + dir.resolve("data.db");
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE schema_version(version INTEGER NOT NULL)");
            for (String sql : Migrations.v1(Dialect.SQLITE)) {
                s.execute(sql);
            }
            s.execute("INSERT INTO schema_version(version) VALUES (1)");
            s.execute("INSERT INTO workbenches(id, owner, world, x, y, z, tier, serial, created_at)"
                    + " VALUES ('b','o','w',1,2,3,'CONSTRUCTOR',1,5)");
        }
        Database.open(Dialect.SQLITE, url, "", "", log).close();
        try (Stream<Path> files = Files.list(dir.resolve("backups"))) {
            Path backup = files.filter(p -> p.getFileName().toString().matches("data-1-.*\\.db"))
                    .findFirst().orElseThrow();
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + backup);
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM workbenches")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM workbenches")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
            assertEquals(Migrations.LATEST, Migrations.currentVersion(c, Dialect.SQLITE));
        }
    }

    @Test void freshDatabaseIsNotBackedUp() throws Exception {
        Database.open(Dialect.SQLITE, "jdbc:sqlite:" + dir.resolve("data.db"), "", "", log).close();
        assertFalse(Files.exists(dir.resolve("backups")));
    }

    @Test void sqliteIsBackedUpBeforeMigratingAnOldSchema() throws Exception {
        String url = "jdbc:sqlite:" + dir.resolve("data.db");
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE old(a INTEGER)");
            s.execute("INSERT INTO old VALUES (42)");
        }
        Database.open(Dialect.SQLITE, url, "", "", log).close();
        try (Stream<Path> files = Files.list(dir.resolve("backups"))) {
            Path backup = files.filter(p -> p.getFileName().toString().matches("data-0-.*\\.db"))
                    .findFirst().orElseThrow();
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + backup);
                 Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT a FROM old")) {
                assertTrue(rs.next());
                assertEquals(42, rs.getInt(1));
            }
        }
    }

    @Test void mysqlLoadsTheDriverItselfAndFailsOnConnectionNotOnMissingDriver() {
        // Puerto cerrado: el error debe venir del driver de MariaDB, no de «No suitable driver».
        java.sql.SQLException e = assertThrows(java.sql.SQLException.class, () -> Database.open(Dialect.MYSQL,
                "jdbc:mariadb://127.0.0.1:1/x?connectTimeout=1000", "u", "p", log));
        assertFalse(String.valueOf(e.getMessage()).contains("No suitable driver"), e.getMessage());
        assertTrue(java.util.Arrays.stream(e.getStackTrace()).anyMatch(f -> f.getClassName().startsWith("org.mariadb.jdbc")),
                "el error debe salir del driver de MariaDB: " + e);
    }

    @Test void v2ProjectsGetANullSourceHashAndV3CanBeRepeated() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("a.db"));
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE schema_version(version INTEGER NOT NULL)");
            for (String sql : Migrations.v1(Dialect.SQLITE)) {
                s.execute(sql);
            }
            for (String sql : Migrations.v2(Dialect.SQLITE)) {
                s.execute(sql);
            }
            s.execute("INSERT INTO schema_version(version) VALUES (2)");
            s.execute("INSERT INTO projects(id, bench_id, structure_id, owner, state, world, x, y, z, rotation,"
                    + " created_at, updated_at) VALUES ('p','b','casa','o','BUILDING','w',0,0,0,'NONE',1,1)");
            Migrations.run(c, Dialect.SQLITE, null, log);
            assertEquals(Migrations.LATEST, Migrations.currentVersion(c, Dialect.SQLITE));
            try (ResultSet rs = s.executeQuery("SELECT source_hash FROM projects WHERE id = 'p'")) {
                assertTrue(rs.next());
                assertNull(rs.getString(1), "obra vieja: sin hash, no se comprueba");
            }
            // Como un MySQL que cortó tras el ALTER: repetir no falla.
            Migrations.v3(c);
            Migrations.v3(c);
        }
    }

    @Test void v4IsRepeatable() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("a.db"));
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE schema_version(version INTEGER NOT NULL)");
            for (String sql : Migrations.v1(Dialect.SQLITE)) {
                s.execute(sql);
            }
            for (String sql : Migrations.v2(Dialect.SQLITE)) {
                s.execute(sql);
            }
            Migrations.v3(c);
            s.execute("INSERT INTO schema_version(version) VALUES (3)");
            s.execute("INSERT INTO projects(id, bench_id, structure_id, owner, state, world, x, y, z, rotation,"
                    + " created_at, updated_at) VALUES ('p','b','casa','o','BUILDING','w',0,0,0,'NONE',1,1)");
            Migrations.run(c, Dialect.SQLITE, null, log);
            assertEquals(Migrations.LATEST, Migrations.currentVersion(c, Dialect.SQLITE));
            try (ResultSet rs = s.executeQuery("SELECT money_cents FROM projects WHERE id = 'p'")) {
                assertTrue(rs.next());
                assertEquals(0, rs.getLong(1), "obra de antes: sin dinero cobrado");
            }
            s.execute("INSERT INTO pending_money(player, amount_cents, created_at) VALUES ('o', 750, 1)");
            // Como un MySQL que cortó a mitad: repetir no falla ni borra lo que hay.
            Migrations.v4(c, Dialect.SQLITE);
            Migrations.v4(c, Dialect.SQLITE);
            try (ResultSet rs = s.executeQuery("SELECT amount_cents, paid_at FROM pending_money")) {
                assertTrue(rs.next());
                assertEquals(750, rs.getLong(1));
                assertNull(rs.getObject(2));
                assertFalse(rs.next());
            }
        }
        for (String sql : Migrations.v4Tables(Dialect.MYSQL)) {
            assertTrue(sql.contains("IF NOT EXISTS"), sql);
        }
    }

    @Test void v5IsRepeatable() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("a.db"));
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE schema_version(version INTEGER NOT NULL)");
            for (String sql : Migrations.v1(Dialect.SQLITE)) {
                s.execute(sql);
            }
            for (String sql : Migrations.v2(Dialect.SQLITE)) {
                s.execute(sql);
            }
            Migrations.v3(c);
            Migrations.v4(c, Dialect.SQLITE);
            s.execute("INSERT INTO schema_version(version) VALUES (4)");
            Migrations.run(c, Dialect.SQLITE, null, log);
            assertEquals(Migrations.LATEST, Migrations.currentVersion(c, Dialect.SQLITE));
            s.execute("INSERT INTO build_modules(build_id, data) VALUES ('b', 'griefprevention:42')");
            // Como un MySQL que cortó a mitad: repetir no falla ni borra lo que hay.
            for (String sql : Migrations.v5(Dialect.SQLITE)) {
                s.execute(sql);
            }
            try (ResultSet rs = s.executeQuery("SELECT data FROM build_modules WHERE build_id = 'b'")) {
                assertTrue(rs.next());
                assertEquals("griefprevention:42", rs.getString(1));
            }
        }
        for (Dialect d : Dialect.values()) {
            for (String sql : Migrations.v5(d)) {
                assertTrue(sql.contains("IF NOT EXISTS"), sql);
            }
        }
    }

    @Test void v6IsRepeatable() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("a.db"));
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE schema_version(version INTEGER NOT NULL)");
            for (String sql : Migrations.v1(Dialect.SQLITE)) {
                s.execute(sql);
            }
            for (String sql : Migrations.v2(Dialect.SQLITE)) {
                s.execute(sql);
            }
            Migrations.v3(c);
            Migrations.v4(c, Dialect.SQLITE);
            for (String sql : Migrations.v5(Dialect.SQLITE)) {
                s.execute(sql);
            }
            s.execute("INSERT INTO schema_version(version) VALUES (5)");
            s.execute("INSERT INTO projects(id, bench_id, structure_id, owner, state, world, x, y, z, rotation,"
                    + " created_at, updated_at) VALUES ('p','b','casa','o','BUILDING','w',0,0,0,'NONE',1,1)");
            s.execute("INSERT INTO builds(id, project_id, structure_id, owner, world, x, y, z, rotation, min_x,"
                    + " min_y, min_z, max_x, max_y, max_z, completed_at) VALUES ('c','p0','casa','o','w',0,0,0,"
                    + "'NONE',0,0,0,1,1,1,1)");
            Migrations.run(c, Dialect.SQLITE, null, log);
            assertEquals(6, Migrations.LATEST);
            assertEquals(Migrations.LATEST, Migrations.currentVersion(c, Dialect.SQLITE));
            // Como un MySQL que cortó a mitad: repetir no falla ni borra lo que hay.
            Migrations.v6(c);
            Migrations.v6(c);
            try (ResultSet rs = s.executeQuery("SELECT kind, build_id, repair_blocks FROM projects"
                    + " WHERE id = 'p'")) {
                assertTrue(rs.next());
                assertEquals("BUILD", rs.getString(1), "obra de antes: una construcción normal");
                assertNull(rs.getObject(2));
                assertEquals(0, rs.getInt(3));
            }
            try (ResultSet rs = s.executeQuery("SELECT source_hash FROM builds WHERE id = 'c'")) {
                assertTrue(rs.next());
                assertNull(rs.getObject(1), "construcción de antes: sin hash (se puede reparar)");
            }
        }
    }
}
