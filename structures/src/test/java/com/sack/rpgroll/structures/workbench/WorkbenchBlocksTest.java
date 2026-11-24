package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.structures.workbench.WorkbenchBlocks.BlockCheck;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkbenchBlocksTest {

    @Test void expectedBlockIsFine() {
        assertEquals(BlockCheck.OK, WorkbenchBlocks.check(false, true, Material.BEACON, Material.BEACON));
        assertEquals(BlockCheck.OK, WorkbenchBlocks.check(false, false, Material.BARRIER, Material.BARRIER));
    }

    @Test void changedFallbackOrModelIsNormalizedNotOrphaned() {
        // fallback-block pasó de BEACON a LODESTONE: la mesa sigue siendo mesa, no un faro gratis.
        assertEquals(BlockCheck.NORMALIZE, WorkbenchBlocks.check(false, true, Material.BEACON, Material.LODESTONE));
        // Se quitó el modelo: la barrera vieja vuelve al bloque del nivel.
        assertEquals(BlockCheck.NORMALIZE, WorkbenchBlocks.check(false, true, Material.BARRIER, Material.BEACON));
        // Se añadió un modelo.
        assertEquals(BlockCheck.NORMALIZE, WorkbenchBlocks.check(false, true, Material.BEACON, Material.BARRIER));
    }

    @Test void airOrUnknownBlockIsOrphaned() {
        assertEquals(BlockCheck.ORPHAN, WorkbenchBlocks.check(true, true, Material.AIR, Material.BEACON));
        assertEquals(BlockCheck.ORPHAN, WorkbenchBlocks.check(false, false, Material.STONE, Material.BEACON));
    }
}
