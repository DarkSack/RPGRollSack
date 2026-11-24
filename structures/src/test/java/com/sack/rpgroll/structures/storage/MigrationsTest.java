package com.sack.rpgroll.structures.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
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
            assertEquals(1, Migrations.currentVersion(c, Dialect.SQLITE));
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
}
