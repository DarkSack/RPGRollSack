package com.sack.rpgroll.worldforge.generation;

import com.sack.rpgroll.worldforge.TestCatalog;
import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.profile.ProfileCodec;
import com.sack.rpgroll.worldforge.terrain.TerrainSpec;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TerrainEngineTest {

    private static TerrainSpec spec(String yaml) {
        var r = TerrainSpec.parse("id: x\n" + yaml);
        assertTrue(r.isOk(), r.toString());
        return r.value();
    }

    private static TerrainEngine engine(String yaml, long seed) {
        return new TerrainEngine(spec(yaml), seed, -64, 320);
    }

    /** Samples a 24 km square every 96 blocks. */
    private static int[] sample(TerrainEngine e) {
        int[] h = new int[250 * 250];
        int i = 0;
        for (int x = 0; x < 250; x++) {
            for (int z = 0; z < 250; z++) {
                h[i++] = e.height(x * 96 - 12000, z * 96 - 12000);
            }
        }
        return h;
    }

    @Test
    void same_seed_same_land_other_seed_other_land() {
        TerrainEngine a = engine("", 42), b = engine("", 42), c = engine("", 43);
        int differ = 0;
        for (int x = -2000; x < 2000; x += 250) {
            for (int z = -2000; z < 2000; z += 250) {
                assertEquals(a.height(x, z), b.height(x, z));
                assertEquals(a.biome(x, 64, z), b.biome(x, 64, z));
                differ += a.height(x, z) != c.height(x, z) ? 1 : 0;
            }
        }
        assertTrue(differ > 100, "another seed changes most columns: " + differ);
    }

    @Test
    void vanilla_like_numbers_give_sea_land_and_mountains() {
        int[] h = sample(engine("", 20261020));
        long sea = java.util.Arrays.stream(h).filter(y -> y < 63).count();
        int max = java.util.Arrays.stream(h).max().orElseThrow(), min = java.util.Arrays.stream(h).min().orElseThrow();
        long high = java.util.Arrays.stream(h).filter(y -> y > 150).count();
        int[] sorted = h.clone();
        java.util.Arrays.sort(sorted);
        String stats = "mar " + sea * 100 / h.length + "% · min " + min + " · max " + max + " · >150: " + high * 1000 / h.length
                + "‰ · p50 " + sorted[h.length / 2] + " · p90 " + sorted[h.length * 9 / 10] + " · p99 " + sorted[h.length * 99 / 100];
        System.out.println("motor de terreno, valores de vanilla: " + stats);
        assertTrue(sea > h.length / 5 && sea < h.length * 7 / 10, stats);
        assertTrue(max > 170 && max <= 312, stats);
        assertTrue(min >= 0, stats);
        assertTrue(high > 0, stats);
    }

    @Test
    void height_scale_stretches_around_y64_and_offset_raises() {
        TerrainEngine one = engine("", 7), two = engine("height-scale: 2\n", 7), up = engine("height-offset: 20\n", 7);
        for (int x = -3000; x < 3000; x += 113) {
            for (int z = -3000; z < 3000; z += 127) {
                int h1 = one.height(x, z), h2 = two.height(x, z);
                if (h2 < 256) { // below the squeeze near the ceiling
                    assertTrue(Math.abs((h2 - 64) - 2 * (h1 - 64)) <= 2, x + "," + z + ": " + h1 + " → " + h2);
                }
                if (h1 + 20 < 256) {
                    assertEquals(h1 + 20, up.height(x, z), 1, x + "," + z);
                }
                assertTrue(h2 <= 312, "never touches the ceiling");
            }
        }
    }

    @Test
    void biomes_follow_the_height() {
        TerrainEngine e = engine("", 99);
        Set<String> seen = new HashSet<>();
        for (int x = -12000; x < 12000; x += 160) {
            for (int z = -12000; z < 12000; z += 160) {
                int h = e.height(x, z);
                String b = e.biome(x, h, z);
                seen.add(b);
                assertTrue(TerrainEngine.BIOMES.contains(b), b);
                if (h < 62) {
                    assertTrue(b.endsWith("ocean"), h + " " + b);
                }
                if (b.endsWith("_peaks")) {
                    assertTrue(h > 200, h + " " + b);
                }
            }
        }
        assertTrue(seen.size() > 15, "variety: " + seen);
        assertTrue(seen.stream().anyMatch(b -> b.endsWith("_peaks")), String.valueOf(seen));
    }

    /** A taller terrain squeezed under the ceiling still has its peak biomes on its peaks. */
    @Test
    void a_tall_terrain_keeps_its_peaks() {
        TerrainEngine e = new TerrainEngine(spec("height-scale: 1.7\npeaks: 1.5\ncontinent-size: 1.5\nbiome-size: 1.5\n"), 99, -64, 320);
        int peaks = 0, highest = 0;
        for (int x = -12000; x < 12000; x += 160) {
            for (int z = -12000; z < 12000; z += 160) {
                int h = e.height(x, z);
                highest = Math.max(highest, h);
                if (e.biome(x, h, z).endsWith("_peaks")) {
                    peaks++;
                    assertTrue(h > 240, "peaks at " + h);
                }
            }
        }
        assertTrue(peaks > 0, "no peak biomes; highest " + highest);
        assertTrue(highest <= 312, "squeezed under the ceiling: " + highest);
    }

    private static final Path SERVER_JAR = Path.of("test-server/.cache/folia/versions/26.1.2/folia-26.1.2.jar");

    @Test
    void every_biome_the_engine_uses_exists_in_26_1_2() throws IOException {
        assumeTrue(Files.exists(SERVER_JAR), "sin " + SERVER_JAR + ": se salta");
        try (ZipFile z = new ZipFile(SERVER_JAR.toFile())) {
            for (String b : TerrainEngine.BIOMES) {
                assertNotEquals(null, z.getEntry("data/minecraft/worldgen/biome/" + b.substring("minecraft:".length()) + ".json"), b);
            }
        }
    }

    private static List<String> errorFields(String yaml) {
        var r = ProfileCodec.read(yaml);
        assertTrue(r.isOk(), r.toString());
        return TestCatalog.validator().validate(r.value()).stream().filter(Problem::isError).map(Problem::field).toList();
    }

    @Test
    void the_terrain_provider_in_a_profile() {
        assertEquals(List.of(), errorFields("schema: 1\nid: x\nprovider: terrain\nterrain: montanas\nseed: 3\n"));
        assertEquals(List.of("terrain"), errorFields("schema: 1\nid: x\nprovider: terrain\n"));
        assertEquals(List.of("terrain"), errorFields("schema: 1\nid: x\nprovider: terrain\nterrain: nada\n"));
        assertEquals(List.of("terrain"), errorFields("schema: 1\nid: x\nprovider: hybrid\nterrain: montanas\n"));
        assertEquals(List.of("environment"), errorFields("schema: 1\nid: x\nprovider: terrain\nterrain: suave\nenvironment: nether\n"));
        assertEquals(List.of("biomes"), errorFields("schema: 1\nid: x\nprovider: terrain\nterrain: suave\nbiomes: [plains]\n"));
        var p = ProfileCodec.read("schema: 1\nid: x\nprovider: terrain\nterrain: suave\n").value();
        assertEquals(p, ProfileCodec.read(ProfileCodec.write(p)).value());
    }
}
