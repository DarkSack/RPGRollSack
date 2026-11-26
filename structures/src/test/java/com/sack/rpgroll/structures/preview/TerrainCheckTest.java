package com.sack.rpgroll.structures.preview;

import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.catalog.PlacementRules;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TerrainCheckTest {

    private static final TerrainCheck.BlockInfo AIR =
            new TerrainCheck.BlockInfo(Material.AIR, true, false, false, () -> "minecraft:plains");
    private static final TerrainCheck.BlockInfo STONE =
            new TerrainCheck.BlockInfo(Material.STONE, false, false, true, () -> "minecraft:plains");
    private static final TerrainCheck.BlockInfo WATER =
            new TerrainCheck.BlockInfo(Material.WATER, true, true, false, () -> "minecraft:plains");

    private static final PlacementRules FREE = new PlacementRules(false, false, List.of(), List.of(), Set.of());

    /** Bloques en x 0..1, y 0..1, z 0 (4 bloques). */
    private static List<SourceBlock> blocks() {
        List<SourceBlock> l = new ArrayList<>();
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                l.add(new SourceBlock(x, y, 0, null, null));
            }
        }
        return l;
    }

    private static CheckResult run(int oy, PlacementRules rules, TerrainCheck.BlockLookup lookup,
                                   List<Footprint> others) {
        return TerrainCheck.check("world", -64, 320, 0, oy, 0, 2, 2, 1, blocks(), rules, lookup, others, () -> null);
    }

    @Test
    void occupiedBlockIsCollision() {
        CheckResult r = run(64, FREE, (x, y, z) -> x == 1 && y == 65 ? STONE : AIR, List.of());
        assertEquals("preview.blocked", r.reason());
        assertEquals(1, r.collisions().size());
        assertArrayEquals(new int[] { 1, 65, 0 }, r.collisions().get(0));
    }

    @Test
    void replaceListIsNotCollision() {
        PlacementRules rules = new PlacementRules(false, false, List.of(), List.of(), Set.of(Material.STONE));
        assertNull(run(64, rules, (x, y, z) -> STONE, List.of()).reason());
    }

    @Test
    void liquidNeedsAllowLiquids() {
        assertEquals("preview.blocked", run(64, FREE, (x, y, z) -> WATER, List.of()).reason());
        PlacementRules ok = new PlacementRules(false, true, List.of(), List.of(), Set.of());
        assertNull(run(64, ok, (x, y, z) -> WATER, List.of()).reason());
    }

    @Test
    void outOfWorldHeightFails() {
        assertEquals("preview.out-of-world", run(-65, FREE, (x, y, z) -> AIR, List.of()).reason());
        assertEquals("preview.out-of-world", run(319, FREE, (x, y, z) -> AIR, List.of()).reason());
        assertNull(run(-64, FREE, (x, y, z) -> AIR, List.of()).reason());
        assertNull(run(318, FREE, (x, y, z) -> AIR, List.of()).reason());
    }

    @Test
    void noGroundFailsWhenFloating() {
        PlacementRules rules = new PlacementRules(true, false, List.of(), List.of(), Set.of());
        assertEquals("preview.no-ground", run(64, rules, (x, y, z) -> AIR, List.of()).reason());
        assertNull(run(64, rules, (x, y, z) -> y == 63 ? STONE : AIR, List.of()).reason());
    }

    @Test
    void overlapWithOtherProjectFails() {
        List<Footprint> others = List.of(Footprint.of(1, 60, 0, 5, 10, 5));
        assertEquals("preview.overlaps-project", run(64, FREE, (x, y, z) -> AIR, others).reason());
        assertNull(run(64, FREE, (x, y, z) -> AIR, List.of(Footprint.of(2, 60, 0, 5, 10, 5))).reason());
    }

    @Test
    void worldBiomeAndProtectionReasons() {
        PlacementRules worlds = new PlacementRules(false, false, List.of("nether"), List.of(), Set.of());
        assertEquals("preview.world-not-allowed", run(64, worlds, (x, y, z) -> AIR, List.of()).reason());
        PlacementRules biomes = new PlacementRules(false, false, List.of(), List.of("desert"), Set.of());
        assertEquals("preview.biome-not-allowed", run(64, biomes, (x, y, z) -> AIR, List.of()).reason());
        PlacementRules plains = new PlacementRules(false, false, List.of(), List.of("PLAINS"), Set.of());
        assertNull(run(64, plains, (x, y, z) -> AIR, List.of()).reason());
        CheckResult denied = TerrainCheck.check("world", -64, 320, 0, 64, 0, 2, 2, 1, blocks(), FREE,
                (x, y, z) -> AIR, List.of(), () -> "protect.worldguard");
        assertEquals("protect.worldguard", denied.reason(), "el motivo es la clave que da la protección");
    }

    @Test
    void biomeIsAskedOnlyOnce() {
        AtomicInteger asked = new AtomicInteger();
        TerrainCheck.BlockInfo air = new TerrainCheck.BlockInfo(Material.AIR, true, false, false, () -> {
            asked.incrementAndGet();
            return "minecraft:plains";
        });
        PlacementRules plains = new PlacementRules(false, false, List.of(), List.of("plains"), Set.of());
        assertNull(run(64, plains, (x, y, z) -> air, List.of()).reason());
        assertEquals(1, asked.get());
    }

    @Test
    void scanSplitsTheWorkByBudget() {
        List<SourceBlock> ten = new ArrayList<>();
        for (int x = 0; x < 10; x++) {
            ten.add(new SourceBlock(x, 0, 0, null, null));
        }
        AtomicInteger looks = new AtomicInteger();
        PlacementRules ground = new PlacementRules(true, false, List.of(), List.of(), Set.of());
        TerrainCheck.Scan scan = TerrainCheck.scan("world", -64, 320, 0, 64, 0, 10, 1, 1, ten, ground,
                (x, y, z) -> {
                    looks.incrementAndGet();
                    return y == 63 ? STONE : AIR;
                }, List.of(), () -> null);
        int steps = 0;
        CheckResult r = null;
        while (r == null) {
            int before = looks.get();
            r = scan.step(4);
            assertTrue(looks.get() - before <= 4, "como mucho el presupuesto por paso");
            steps++;
        }
        assertTrue(r.ok(), r.reason());
        assertEquals(20, looks.get(), "10 choques + 10 de suelo");
        assertEquals(5, steps);
        assertSame(r, scan.step(4), "terminado: no vuelve a mirar");
        assertEquals(20, looks.get());
    }

    @Test
    void scanGivesTheSameResultAsCheck() {
        TerrainCheck.BlockLookup lookup = (x, y, z) -> x == 1 && y == 65 ? STONE : AIR;
        TerrainCheck.Scan scan = TerrainCheck.scan("world", -64, 320, 0, 64, 0, 2, 2, 1, blocks(), FREE, lookup,
                List.of(), () -> null);
        CheckResult r = null;
        while (r == null) {
            r = scan.step(1);
        }
        assertEquals("preview.blocked", r.reason());
        assertEquals(run(64, FREE, lookup, List.of()).collisions().size(), r.collisions().size());
    }
}
