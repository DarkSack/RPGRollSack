package com.sack.rpgroll.items.ore;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OreMathTest {

    @Test
    void breakSpeedMatchesTheOreInsteadOfTheDisguise() {
        // Pico de diamante (8) contra una mena de dureza 4, disfrazada de bloque musical (0,8, velocidad 1).
        double multiplier = OreMath.breakSpeedMultiplier(1, 0.8, 8, 4, true);
        double ticks = 1 / (1 / 0.8 / 30 * multiplier);
        assertEquals(15.0, ticks, 1e-9);

        // Sin poder recogerla tarda más de tres veces lo mismo (100 en lugar de 30 por unidad de dureza).
        double slow = OreMath.breakSpeedMultiplier(1, 0.8, 8, 4, false);
        assertEquals(multiplier * 0.3, slow, 1e-9);
    }

    @Test
    void efficiencyAddsLikeVanilla() {
        assertEquals(8, OreMath.withEfficiency(8, 0));
        assertEquals(34, OreMath.withEfficiency(8, 5));
    }

    @Test
    void fortuneNeverLowersAndStaysInRange() {
        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            int drops = OreMath.fortune(1, 3, random);
            assertTrue(drops >= 1 && drops <= 4, "fuera de rango: " + drops);
        }
        assertEquals(2, OreMath.fortune(2, 0, random));
    }

    @Test
    void veinsStayInsideTheChunkAndHeightWithoutRepeats() {
        Random random = new Random(7);
        for (int n = 0; n < 200; n++) {
            List<int[]> vein = OreMath.vein(random, random.nextInt(16), 10, random.nextInt(16), 7, 5, 12);
            Set<String> seen = new HashSet<>();
            for (int[] p : vein) {
                assertTrue(p[0] >= 0 && p[0] <= 15 && p[2] >= 0 && p[2] <= 15);
                assertTrue(p[1] >= 5 && p[1] <= 12);
                assertTrue(seen.add(p[0] + "," + p[1] + "," + p[2]));
            }
            assertTrue(vein.size() <= 7 && !vein.isEmpty());
        }
    }

    @Test
    void veinCountUsesTheFractionAsChance() {
        Random random = new Random(3);
        int total = 0;
        for (int i = 0; i < 10000; i++) {
            total += OreMath.veinCount(0.25, random);
        }
        assertEquals(2500, total, 200);
        assertEquals(3, OreMath.veinCount(3.0, random));
    }

    @Test
    void parserReadsAnOre() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                id: mena_de_prueba
                blocks:
                  stone: "minecraft:note_block[instrument=zombie,note=0,powered=false]"
                  deepslate: "minecraft:note_block[instrument=zombie,note=1,powered=false]"
                drop:
                  item: prueba_en_bruto
                xp: [1, 3]
                hardness: 4.5
                required-tier: 3
                generation:
                  - worlds: [recursos]
                    min-y: -16
                    max-y: 40
                    veins-per-chunk: 2.5
                    vein-size: [2, 5]
                    replace:
                      stone: stone
                      DEEPSLATE: deepslate
                """);

        OreDefinition ore = new OreParser().parse(config);

        assertEquals("prueba_en_bruto", ore.dropItem());
        assertEquals(1, ore.dropMin());
        assertEquals(3, ore.xpMax());
        assertEquals(3, ore.requiredTier());
        assertEquals(2, ore.blocks().size());
        var generation = ore.generation().get(0);
        assertEquals(2.5, generation.veinsPerChunk());
        assertEquals(5, generation.veinMax());
        assertEquals("stone", generation.replace().get("STONE"));
    }

    @Test
    void parserRejectsAReplaceWithAnUnknownVariant() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                id: mala
                blocks:
                  stone: "minecraft:note_block[instrument=zombie,note=0,powered=false]"
                drop:
                  item: algo
                generation:
                  - worlds: [recursos]
                    replace:
                      NETHERRACK: netherrack
                """);

        assertThrows(IllegalArgumentException.class, () -> new OreParser().parse(config));
    }

}
