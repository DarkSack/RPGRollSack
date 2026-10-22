package com.sack.rpgroll.worldforge.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageTest {

    @TempDir
    Path dir;

    @Test
    void a_new_database_migrates_to_the_latest_schema_and_keeps_data() throws Exception {
        Path file = dir.resolve("worldforge.db");
        try (Database db = Database.open(file)) {
            assertEquals(Database.MIGRATIONS.size(), db.schemaVersion());
            Records r = new Records(db);
            r.saveWorld(new Records.ManagedWorld("w1", "flat", "abc", "READY", 1L, "tester")).get();
            r.history("tester", "world.create", "w1", null).get();
        }
        try (Database db = Database.open(file)) {
            Records r = new Records(db);
            assertEquals(Optional.of("flat"), r.world("w1").get().map(Records.ManagedWorld::profileId));
            assertEquals("world.create", r.recentHistory(5).get().getFirst().action());
            assertEquals(Database.MIGRATIONS.size(), db.schemaVersion());
        }
    }

    @Test
    void a_database_from_a_newer_worldforge_is_not_touched() throws Exception {
        Path file = dir.resolve("newer.db");
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE schema_version(version INTEGER NOT NULL)");
            s.execute("INSERT INTO schema_version VALUES (999)");
        }
        SQLException e = assertThrows(SQLException.class, () -> Database.open(file));
        assertTrue(e.getMessage().contains("más nuevo"), e.getMessage());
    }

    @Test
    void a_database_from_the_previous_version_migrates_and_keeps_its_rows() throws Exception {
        Path file = dir.resolve("v1.db");
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); Statement s = c.createStatement()) {
            for (String sql : Database.MIGRATIONS.getFirst()) {
                s.execute(sql);
            }
            s.execute("CREATE TABLE schema_version(version INTEGER NOT NULL)");
            s.execute("INSERT INTO schema_version VALUES (1)");
            s.execute("INSERT INTO dimensions(id, profile_id, state, message, updated_at) VALUES ('vieja', 'p', 'READY', null, 1)");
        }
        try (Database db = Database.open(file)) {
            assertEquals(Database.MIGRATIONS.size(), db.schemaVersion());
            assertEquals("READY|", db.call(c -> {
                try (var rs = c.createStatement().executeQuery("SELECT state, dimension_type FROM dimensions WHERE id = 'vieja'")) {
                    rs.next();
                    return rs.getString(1) + "|" + rs.getString(2);
                }
            }));
        }
    }

    @Test
    void a_file_that_is_not_a_database_gives_a_readable_error() throws Exception {
        Path file = dir.resolve("broken.db");
        Files.write(file, "esto no es sqlite, es texto que alguien pegó aquí ...................................".repeat(20).getBytes());
        SQLException e = assertThrows(SQLException.class, () -> Database.open(file));
        assertTrue(e.getMessage().contains("no es una base de datos") || e.getMessage().contains("dañada"), e.getMessage());
    }

    @Test
    void saving_a_world_again_updates_it() throws Exception {
        try (Database db = Database.open(dir.resolve("u.db"))) {
            Records r = new Records(db);
            r.saveWorld(new Records.ManagedWorld("w", "flat", "1", "READY", 1L, "a")).get();
            r.saveWorld(new Records.ManagedWorld("w", "empty", "2", "READY", 1L, "a")).get();
            r.setWorldStatus("w", "UNLOADED").get();
            List<Records.ManagedWorld> all = r.worlds().get();
            assertEquals(1, all.size());
            assertEquals("empty", all.getFirst().profileId());
            assertEquals("UNLOADED", all.getFirst().status());
        }
    }

    @Test
    void file_names_from_outside_cannot_leave_their_folder() {
        for (String bad : List.of("../x.yml", "..\\x.yml", "/etc/passwd", "C:\\x.yml", "a/b.yml", ".hidden",
                "", "x..yml", "con:x")) {
            assertFalse(SafeFiles.child(dir, bad).isPresent(), "debería rechazar «" + bad + "»");
        }
        assertEquals(dir.toAbsolutePath().normalize().resolve("perfil-1.yml"), SafeFiles.child(dir, "perfil-1.yml").orElseThrow());
        assertTrue(SafeFiles.within(dir, dir.resolve("a/b")));
        assertFalse(SafeFiles.within(dir, dir.resolve("../fuera")));
    }

    @Test
    void atomic_write_replaces_the_whole_file() throws Exception {
        Path f = dir.resolve("sub/conf.yml");
        SafeFiles.writeAtomic(f, "uno");
        SafeFiles.writeAtomic(f, "dos");
        assertEquals("dos", Files.readString(f));
        assertFalse(Files.exists(f.resolveSibling("conf.yml.tmp")));
    }
}
