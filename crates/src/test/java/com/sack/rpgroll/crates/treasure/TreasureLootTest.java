package com.sack.rpgroll.crates.treasure;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreasureLootTest {

    private static Treasure.Loot loot(int min, int max, Treasure.Entry... entries) {
        return new Treasure.Loot(min, max, List.of(entries));
    }

    @Test
    void rollsBetweenMinAndMaxTimes() {

        Treasure.Loot loot = loot(2, 5, new Treasure.Entry("GOLD_INGOT", 1, 1, 1));
        Random random = new Random(7);
        for (int i = 0; i < 500; i++) {
            int size = TreasureLoot.roll(loot, random).size();
            assertTrue(size >= 2 && size <= 5, "tiradas fuera de rango: " + size);
        }
    }

    @Test
    void amountsStayInRange() {

        Treasure.Loot loot = loot(1, 1, new Treasure.Entry("EMERALD", 3, 8, 1));
        Random random = new Random(3);
        for (int i = 0; i < 500; i++) {
            TreasureLoot.Drop drop = TreasureLoot.roll(loot, random).getFirst();
            assertEquals("EMERALD", drop.item());
            assertTrue(drop.amount() >= 3 && drop.amount() <= 8, "cantidad fuera de rango: " + drop.amount());
        }
    }

    @Test
    void followsWeights() {

        Treasure.Loot loot = loot(1, 1,
                new Treasure.Entry("COMUN", 1, 1, 9),
                new Treasure.Entry("RARO", 1, 1, 1));
        Random random = new Random(11);
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 10_000; i++) {
            counts.merge(TreasureLoot.roll(loot, random).getFirst().item(), 1, Integer::sum);
        }
        double rare = counts.getOrDefault("RARO", 0) / 10_000.0;
        assertTrue(rare > 0.08 && rare < 0.12, "RARO debería salir ~10 %: " + rare);
    }

    @Test
    void resolveSkipsUnknownItemsAndWarnsOnce() {

        List<TreasureLoot.Drop> drops = List.of(
                new TreasureLoot.Drop("DIAMOND", 2),
                new TreasureLoot.Drop("mareas_no_existe", 1),
                new TreasureLoot.Drop("mareas_no_existe", 3),
                new TreasureLoot.Drop("EMERALD", 5));
        List<String> warnings = new ArrayList<>();

        List<String> items = TreasureLoot.resolve(drops,
                (id, amount) -> id.startsWith("mareas_") ? Optional.empty() : Optional.of(id + "x" + amount),
                warnings::add);

        assertEquals(List.of("DIAMONDx2", "EMERALDx5"), items);
        assertEquals(1, warnings.size(), "un aviso por id, no por tirada");
        assertTrue(warnings.getFirst().contains("mareas_no_existe"));
    }

}
