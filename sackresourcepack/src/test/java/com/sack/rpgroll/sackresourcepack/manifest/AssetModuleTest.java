package com.sack.rpgroll.sackresourcepack.manifest;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetModuleTest {

    private final File directory = new File("content/example");

    @Test
    void constructorRejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new AssetModule(null, "Example", null, null, null, 0, null, null, null, directory));
    }

    @Test
    void constructorRejectsNullDirectory() {
        assertThrows(NullPointerException.class,
                () -> new AssetModule("example", "Example", null, null, null, 0, null, null, null, null));
    }

    @Test
    void blankNameFallsBackToId() {
        AssetModule module = new AssetModule("example", "  ", null, null, null, 0, null, null, null, directory);
        assertEquals("example", module.name());
    }

    @Test
    void blankVersionDefaultsTo100() {
        AssetModule module = new AssetModule("example", "Example", "", null, null, 0, null, null, null, directory);
        assertEquals("1.0.0", module.version());
    }

    @Test
    void blankNamespaceFallsBackToId() {
        AssetModule module = new AssetModule("example", "Example", null, null, "  ", 0, null, null, null, directory);
        assertEquals("example", module.namespace());
    }

    @Test
    void nullDependsAndOptionalBecomeEmptyLists() {
        AssetModule module = new AssetModule("example", "Example", null, null, null, 0, null, null, null, directory);

        assertTrue(module.depends().isEmpty());
        assertTrue(module.optional().isEmpty());
    }

    @Test
    void assetsDirectoryIsAssetsSubfolderOfModuleDirectory() {
        AssetModule module = new AssetModule("example", "Example", null, null, null, 0, null, null, null, directory);
        assertEquals(new File(directory, "assets"), module.assetsDirectory());
    }

    @Test
    void dataDirectoryIsDataSubfolderOfModuleDirectory() {
        AssetModule module = new AssetModule("example", "Example", null, null, null, 0, null, null, null, directory);
        assertEquals(new File(directory, "data"), module.dataDirectory());
    }
}
