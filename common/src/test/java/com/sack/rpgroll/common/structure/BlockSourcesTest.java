package com.sack.rpgroll.common.structure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BlockSourcesTest {

    @Test
    void unknownExtensionIsRejected(@TempDir Path d) {
        IOException e = assertThrows(IOException.class, () -> BlockSources.read(d.resolve("a.txt").toFile(), p -> true));
        assertTrue(e.getMessage().contains("Formato no soportado"));
    }

    @Test
    void schemWithoutWorldEditIsRejected(@TempDir Path d) {
        for (String name : new String[] { "a.schem", "a.schematic" }) {
            File f = d.resolve(name).toFile();
            IOException e = assertThrows(IOException.class, () -> BlockSources.read(f, p -> false));
            assertTrue(e.getMessage().contains("WorldEdit o FastAsyncWorldEdit"));
        }
    }

    @Test
    void schemAvailableAcceptsEitherPlugin() {
        assertTrue(BlockSources.schemAvailable(p -> p.equals("WorldEdit")));
        assertTrue(BlockSources.schemAvailable(p -> p.equals("FastAsyncWorldEdit")));
        assertFalse(BlockSources.schemAvailable(p -> false));
    }
}
