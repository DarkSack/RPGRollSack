package com.sack.rpgroll.structures.storage;

import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

import static com.sack.rpgroll.structures.workbench.WorkbenchTier.CONSTRUCTOR;
import static com.sack.rpgroll.structures.workbench.WorkbenchTier.INGENIERO;
import static org.junit.jupiter.api.Assertions.*;

class WorkbenchRepositoryTest {

    @TempDir Path dir;
    Database db;
    WorkbenchRepository repo;

    String url() {
        return "jdbc:sqlite:" + dir.resolve("data.db");
    }

    @BeforeEach void setUp() throws Exception {
        db = Database.open(Dialect.SQLITE, url(), "", "", Logger.getAnonymousLogger());
        repo = new WorkbenchRepository(db, Dialect.SQLITE);
    }

    @AfterEach void tearDown() {
        db.close();
    }

    static WorkbenchRecord rec(WorkbenchTier tier, int x, long serial) {
        return new WorkbenchRecord(UUID.randomUUID(), UUID.randomUUID(), "world", x, 64, 5, tier, serial, false, 1000L);
    }

    @Test void insertFindAndFindAt() {
        WorkbenchRecord r = rec(CONSTRUCTOR, 1, 1);
        repo.insert(r).join();
        assertEquals(Optional.of(r), repo.find(r.id()).join());
        assertEquals(Optional.of(r), repo.findAt("world", 1, 64, 5).join());
        assertTrue(repo.findAt("world", 2, 64, 5).join().isEmpty());
        assertEquals(1, repo.all().join().size());
    }

    @Test void upgradeOnlyOnceFromTheExpectedTier() {
        WorkbenchRecord r = rec(CONSTRUCTOR, 1, 1);
        repo.insert(r).join();
        assertTrue(repo.upgrade(r.id(), CONSTRUCTOR, INGENIERO).join());
        assertFalse(repo.upgrade(r.id(), CONSTRUCTOR, INGENIERO).join());
        assertEquals(INGENIERO, repo.find(r.id()).join().orElseThrow().tier());
    }

    @Test void twoBenchesCannotShareALocation() {
        repo.insert(rec(CONSTRUCTOR, 1, 1)).join();
        CompletionException e = assertThrows(CompletionException.class, () -> repo.insert(rec(CONSTRUCTOR, 1, 2)).join());
        // La causa es la restricción UNIQUE de la base, no otro fallo cualquiera.
        assertInstanceOf(java.sql.SQLException.class, e.getCause());
        assertTrue(String.valueOf(e.getCause().getMessage()).contains("UNIQUE"), e.getCause().getMessage());
    }

    @Test void moveRemoveAndOrphan() {
        WorkbenchRecord r = rec(CONSTRUCTOR, 1, 1);
        repo.insert(r).join();
        assertTrue(repo.markOrphan(r.id()).join());
        assertTrue(repo.find(r.id()).join().orElseThrow().orphan());
        assertTrue(repo.move(r.id(), "nether", 7, 8, 9).join());
        assertFalse(repo.findAt("nether", 7, 8, 9).join().orElseThrow().orphan());
        assertTrue(repo.remove(r.id()).join());
        assertFalse(repo.remove(r.id()).join());
        assertTrue(repo.find(r.id()).join().isEmpty());
    }

    @Test void trustAndUntrust() {
        WorkbenchRecord r = rec(CONSTRUCTOR, 1, 1);
        repo.insert(r).join();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertTrue(repo.trust(r.id(), a).join());
        assertFalse(repo.trust(r.id(), a).join()); // duplicado no falla y dice que ya estaba
        repo.trust(r.id(), b).join();
        assertEquals(Set.of(a, b), repo.trusted(r.id()).join());
        repo.untrust(r.id(), a).join();
        assertEquals(Set.of(b), repo.trusted(r.id()).join());
        repo.remove(r.id()).join(); // cascada
        assertTrue(repo.trusted(r.id()).join().isEmpty());
    }

    @Test void nextSerialIsConsecutiveAndSurvivesReopen() throws Exception {
        long a = repo.nextSerial().join();
        long b = repo.nextSerial().join();
        assertEquals(a + 1, b);
        db.close();
        db = Database.open(Dialect.SQLITE, url(), "", "", Logger.getAnonymousLogger());
        repo = new WorkbenchRepository(db, Dialect.SQLITE);
        assertEquals(b + 1, repo.nextSerial().join());
    }

    @Test void workRunsOffTheCallerThread() {
        assertNotEquals(Thread.currentThread().getName(), db.submit(c -> Thread.currentThread().getName()).join());
        assertEquals("RPGRoll-Structures-DB", db.submit(c -> Thread.currentThread().getName()).join());
    }
}
