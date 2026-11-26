package com.sack.rpgroll.structures.project;

import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.build.BlockWriter;
import com.sack.rpgroll.structures.build.PlacementPlan;
import com.sack.rpgroll.structures.catalog.MaterialSpec;
import com.sack.rpgroll.structures.storage.Box;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepairMathTest {

    static BlockData data(Material m) {
        BlockData d = mock(BlockData.class);
        when(d.getMaterial()).thenReturn(m);
        return d;
    }

    @Test void countsOnlyAirOrReplaceableMismatches() {
        BlockData stone = data(Material.STONE);
        PlacementPlan plan = new PlacementPlan(List.of(new SourceBlock(0, 0, 0, stone, null),
                new SourceBlock(1, 0, 0, stone, null), new SourceBlock(2, 0, 0, stone, null),
                new SourceBlock(3, 0, 0, stone, null)));
        BlockWriter w = mock(BlockWriter.class);
        when(w.chunkLoaded(anyString(), anyInt(), anyInt())).thenReturn(true);
        // (10,64,20) igual; (11) aire; (12) hierba (reemplazable); (13) bloque de otro: no falta, no se pisa.
        when(w.read("world", 10, 64, 20)).thenReturn(new BlockWriter.BlockInfo(Material.STONE, false, stone));
        when(w.read("world", 11, 64, 20)).thenReturn(new BlockWriter.BlockInfo(Material.AIR, false, null));
        BlockData grass = data(Material.SHORT_GRASS), cobble = data(Material.COBBLESTONE);
        when(w.read("world", 12, 64, 20)).thenReturn(new BlockWriter.BlockInfo(Material.SHORT_GRASS, true, grass));
        when(w.read("world", 13, 64, 20)).thenReturn(new BlockWriter.BlockInfo(Material.COBBLESTONE, false, cobble));
        assertEquals(new RepairMath.Result(2, 4), RepairMath.of(plan, w, "world", 10, 64, 20));
    }

    @Test void priceRoundsUp() {
        List<MaterialSpec> specs = List.of(new MaterialSpec(List.of("minecraft:oak_planks"), 8, false),
                new MaterialSpec(List.of("minecraft:torch"), 2, true),
                new MaterialSpec(List.of("minecraft:oak_planks", "minecraft:spruce_planks"), 3, false));
        // Por material (las dos de tablones comparten clave: 11): ceil(11 × 5 / 12) = 5, ceil(2 × 5 / 12) = 1.
        assertEquals(Map.of("minecraft:oak_planks", 5, "minecraft:torch", 1), RepairMath.price(specs, 5, 12));
        assertEquals(Map.of("minecraft:oak_planks", 11, "minecraft:torch", 2), RepairMath.price(specs, 12, 12));
        assertEquals(417, RepairMath.money(1000, 5, 12));
        assertEquals(0, RepairMath.money(0, 5, 12));
    }

    @Test void loadedNeedsEveryChunkOfTheBox() {
        BlockWriter w = mock(BlockWriter.class);
        when(w.chunkLoaded(anyString(), anyInt(), anyInt())).thenReturn(true);
        Box box = new Box(10, 64, 20, 40, 70, 20); // chunks x 0, 1 y 2
        assertTrue(RepairMath.loaded(w, "world", box));
        when(w.chunkLoaded(eq("world"), eq(32), anyInt())).thenReturn(false);
        assertFalse(RepairMath.loaded(w, "world", box));
    }

    @Test void countsReplaceListAsMissingAndNeverReadsUnloadedChunks() {
        BlockData stone = data(Material.STONE), dirt = data(Material.DIRT);
        PlacementPlan plan = new PlacementPlan(List.of(new SourceBlock(0, 0, 0, stone, null),
                new SourceBlock(20, 0, 0, stone, null)));
        BlockWriter w = mock(BlockWriter.class);
        when(w.chunkLoaded(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(w.read(eq("world"), anyInt(), anyInt(), anyInt()))
                .thenReturn(new BlockWriter.BlockInfo(Material.DIRT, false, dirt));
        // La tierra de la lista replace: el motor la pisa, así que falta.
        assertEquals(new RepairMath.Result(2, 2), RepairMath.of(plan, w, "world", 0, 64, 0, Set.of(Material.DIRT)));
        assertEquals(new RepairMath.Result(0, 2), RepairMath.of(plan, w, "world", 0, 64, 0, Set.of()));
        clearInvocations(w);
        when(w.chunkLoaded("world", 20, 0)).thenReturn(false);
        ProjectService.RepairException e = assertThrows(ProjectService.RepairException.class,
                () -> RepairMath.of(plan, w, "world", 0, 64, 0, Set.of()));
        assertEquals("repair.not-loaded", e.key());
        verify(w, never()).read("world", 20, 64, 0);
    }
}
