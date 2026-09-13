package com.sack.rpgroll.common.resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceSyncTest {

    private static final String PATH = "traps/spikes.yml";

    @TempDir
    Path dataFolder;

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private String onDisk() throws IOException {
        return Files.readString(dataFolder.resolve(PATH));
    }

    /** Simula un arranque: registro nuevo leído de disco, sync y guardado. */
    private ResourceSync.Outcome boot(String packaged) throws IOException {
        ResourceSync sync = new ResourceSync(dataFolder);
        ResourceSync.Outcome outcome = sync.sync(PATH, bytes(packaged));
        sync.save();
        return outcome;
    }

    @Test
    void createsMissingFile() throws IOException {
        assertEquals(ResourceSync.Outcome.CREATED, boot("damage: 2"));
        assertEquals("damage: 2", onDisk());
    }

    @Test
    void untouchedFileIsUpdatedToTheNewVersion() throws IOException {
        boot("damage: 2");

        assertEquals(ResourceSync.Outcome.UPDATED, boot("damage: 3"));
        assertEquals("damage: 3", onDisk());
    }

    @Test
    void editedFileIsNeverOverwritten() throws IOException {
        boot("damage: 2");
        Files.writeString(dataFolder.resolve(PATH), "damage: 99");

        assertEquals(ResourceSync.Outcome.KEPT_EDITED, boot("damage: 3"));
        assertEquals("damage: 99", onDisk());
    }

    @Test
    void newVersionOfAnEditedFileIsLeftAsideToCompare() throws IOException {
        boot("damage: 2");
        Files.writeString(dataFolder.resolve(PATH), "damage: 99");

        ResourceSync sync = new ResourceSync(dataFolder);
        sync.sync(PATH, bytes("damage: 3"));

        assertEquals("damage: 3", Files.readString(sync.updateCopy(PATH)));
    }

    @Test
    void theSameNewVersionIsOnlyAnnouncedOnce() throws IOException {
        boot("damage: 2");
        Files.writeString(dataFolder.resolve(PATH), "damage: 99");

        assertEquals(ResourceSync.Outcome.KEPT_EDITED, boot("damage: 3"));
        // Reinicios siguientes con el mismo JAR: nada nuevo que contar.
        assertEquals(ResourceSync.Outcome.UNCHANGED, boot("damage: 3"));
        assertEquals(ResourceSync.Outcome.UNCHANGED, boot("damage: 3"));
        // Pero una versión aún más nueva sí se anuncia.
        assertEquals(ResourceSync.Outcome.KEPT_EDITED, boot("damage: 4"));
        assertEquals("damage: 99", onDisk());
    }

    @Test
    void editedFileWithNothingNewLeavesNoCopyAside() throws IOException {
        boot("damage: 2");
        Files.writeString(dataFolder.resolve(PATH), "damage: 99");

        ResourceSync sync = new ResourceSync(dataFolder);

        assertEquals(ResourceSync.Outcome.UNCHANGED, sync.sync(PATH, bytes("damage: 2")));
        assertFalse(Files.exists(sync.updateCopy(PATH)));
    }

    @Test
    void editedFileStaysProtectedAcrossSeveralUpdates() throws IOException {
        boot("damage: 2");
        Files.writeString(dataFolder.resolve(PATH), "damage: 99");

        boot("damage: 3");
        boot("damage: 4");

        assertEquals("damage: 99", onDisk());
    }

    @Test
    void legacyFileMatchingTheJarIsAdoptedAndThenUpdates() throws IOException {
        // Servidor anterior al registro: el archivo existe y no hay huella.
        Files.createDirectories(dataFolder.resolve("traps"));
        Files.writeString(dataFolder.resolve(PATH), "damage: 2");

        assertEquals(ResourceSync.Outcome.UNCHANGED, boot("damage: 2"));
        assertEquals(ResourceSync.Outcome.UPDATED, boot("damage: 3"));
        assertEquals("damage: 3", onDisk());
    }

    @Test
    void legacyFileDifferingFromTheJarIsTreatedAsEdited() throws IOException {
        Files.createDirectories(dataFolder.resolve("traps"));
        Files.writeString(dataFolder.resolve(PATH), "damage: 1");

        assertEquals(ResourceSync.Outcome.KEPT_EDITED, boot("damage: 2"));
        assertEquals("damage: 1", onDisk());
    }

    @Test
    void applyingTheUpdateByHandCleansTheCopyAside() throws IOException {
        boot("damage: 2");
        Files.writeString(dataFolder.resolve(PATH), "damage: 99");
        boot("damage: 3");

        // El admin copia la versión nueva encima de la suya.
        Files.writeString(dataFolder.resolve(PATH), "damage: 3");

        ResourceSync sync = new ResourceSync(dataFolder);
        assertEquals(ResourceSync.Outcome.UNCHANGED, sync.sync(PATH, bytes("damage: 3")));
        assertFalse(Files.exists(sync.updateCopy(PATH)));

        sync.save();
        // Y a partir de ahí vuelve a actualizarse solo.
        assertEquals(ResourceSync.Outcome.UPDATED, boot("damage: 4"));
    }

    @Test
    void lostManifestFallsBackToNeverOverwriting() throws IOException {
        boot("damage: 2");

        Path manifest = dataFolder.resolve(ResourceSync.INTERNAL_DIR).resolve(ResourceSync.MANIFEST);
        Files.delete(manifest);

        assertEquals(ResourceSync.Outcome.KEPT_EDITED, boot("damage: 3"));
        assertEquals("damage: 2", onDisk());
        assertTrue(Files.exists(manifest));
    }
}
