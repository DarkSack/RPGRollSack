package com.sack.rpgroll.worldforge.generation;

import com.sack.rpgroll.worldforge.api.Capability;
import com.sack.rpgroll.worldforge.api.Ids;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationMathTest {

    @Test
    void ore_heights_stay_in_range_and_triangle_peaks_in_the_middle() {
        Random r = new Random(1);
        int middle = 0, edges = 0;
        for (int i = 0; i < 20_000; i++) {
            int u = OrePopulator.pickY(r, -64, 16, "uniform");
            int t = OrePopulator.pickY(r, -64, 16, "triangle");
            assertTrue(u >= -64 && u <= 16 && t >= -64 && t <= 16);
            if (Math.abs(t - (-24)) <= 8) {
                middle++;
            }
            if (t < -56 || t > 8) {
                edges++;
            }
        }
        assertTrue(middle > edges * 3, "middle=" + middle + " edges=" + edges);
    }

    @Test
    void biome_cells_are_stable_per_seed_and_in_range() {
        for (int x = -50; x < 50; x++) {
            int a = WorldForgeGenerator.CellBiomes.cell(42L, x, -x, 3);
            assertEquals(a, WorldForgeGenerator.CellBiomes.cell(42L, x, -x, 3));
            assertTrue(a >= 0 && a < 3);
        }
    }

    @Test
    void providers_declare_only_what_was_tested() {
        Providers p = new Providers();
        assertEquals(Set.of("vanilla", "empty", "flat", "hybrid", "terrain", "plugin"), p.all().keySet());
        assertEquals(Set.of(Capability.SEED, Capability.ENVIRONMENT, Capability.SPAWN, Capability.PLATFORM),
                p.get("plugin").orElseThrow().capabilities(), "around another plugin's generator only what WorldForge applies itself");
        assertFalse(p.get("vanilla").orElseThrow().capabilities().contains(Capability.ORES));
        assertFalse(p.get("empty").orElseThrow().capabilities().contains(Capability.STRUCTURES));
        assertTrue(p.get("hybrid").orElseThrow().capabilities().contains(Capability.ORES));
    }

    @Test
    void job_ids_and_tokens_have_their_shape() {
        for (int i = 0; i < 1000; i++) {
            assertTrue(Ids.isJobId(Ids.newJobId()));
            assertEquals(6, Ids.newToken().length());
        }
        assertFalse(Ids.isJobId("job-../../x"));
    }
}
