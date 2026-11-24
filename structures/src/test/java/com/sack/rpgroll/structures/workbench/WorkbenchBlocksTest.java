package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.WorkbenchBlocks.BlockCheck;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WorkbenchBlocksTest {

    @Test void expectedBlockIsFine() {
        assertEquals(BlockCheck.OK, WorkbenchBlocks.check(false, true, Material.BEACON, Material.BEACON, Material.BEACON));
        assertEquals(BlockCheck.OK, WorkbenchBlocks.check(false, false, Material.BARRIER, null, Material.BARRIER));
    }

    @Test void changedFallbackOrModelIsNormalizedNotOrphaned() {
        // fallback-block pasó de BEACON a LODESTONE: la mesa sigue siendo mesa, no un faro gratis.
        assertEquals(BlockCheck.NORMALIZE,
                WorkbenchBlocks.check(false, true, Material.BEACON, Material.BEACON, Material.LODESTONE));
        // Se quitó el modelo: la barrera vieja vuelve al bloque del nivel.
        assertEquals(BlockCheck.NORMALIZE,
                WorkbenchBlocks.check(false, true, Material.BARRIER, Material.BARRIER, Material.BEACON));
        // Se añadió un modelo.
        assertEquals(BlockCheck.NORMALIZE,
                WorkbenchBlocks.check(false, true, Material.BEACON, Material.BEACON, Material.BARRIER));
    }

    @Test void aPlayersBlockInTheGapIsNeverOverwritten() {
        // La mesa se quitó con WorldEdit y alguien puso un cofre en el hueco: huérfana, el cofre se queda.
        assertEquals(BlockCheck.ORPHAN,
                WorkbenchBlocks.check(false, true, Material.CHEST, Material.BEACON, Material.LODESTONE));
        // Entrada del formato viejo (sin material): solo la barrera se repone.
        assertEquals(BlockCheck.ORPHAN, WorkbenchBlocks.check(false, true, Material.CHEST, null, Material.BEACON));
        assertEquals(BlockCheck.NORMALIZE, WorkbenchBlocks.check(false, true, Material.BARRIER, null, Material.BEACON));
    }

    @Test void airOrUnknownBlockIsOrphaned() {
        assertEquals(BlockCheck.ORPHAN, WorkbenchBlocks.check(true, true, Material.AIR, Material.BEACON, Material.BEACON));
        assertEquals(BlockCheck.ORPHAN, WorkbenchBlocks.check(false, false, Material.STONE, null, Material.BEACON));
        assertEquals(BlockCheck.ORPHAN, WorkbenchBlocks.check(false, false, Material.BARRIER, null, Material.BEACON));
    }

    @Test void chunkEntriesKeepTheMaterialAndStillReadTheOldFormat() {
        UUID id = UUID.randomUUID();
        WorkbenchRecord r = new WorkbenchRecord(id, UUID.randomUUID(), "world", -3, 64, 17,
                WorkbenchTier.ARQUITECTO, 1, false, 1);
        String entry = BenchChunks.entry(r, Material.BEACON);
        assertEquals(id + "@-3,64,17:BEACON", entry);

        BenchChunks.Entry now = BenchChunks.parse(entry);
        assertEquals(id.toString(), now.id());
        assertEquals("-3", now.x());
        assertEquals("17", now.z());
        assertEquals(Material.BEACON, now.material());

        BenchChunks.Entry old = BenchChunks.parse(id + "@-3,64,17");
        assertEquals(id.toString(), old.id());
        assertEquals("64", old.y());
        assertNull(old.material());

        BenchChunks.Entry junk = BenchChunks.parse("basura");
        assertEquals("", junk.id());
        assertEquals("?", junk.x());
    }
}
