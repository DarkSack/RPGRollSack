package com.sack.rpgroll.items.core;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemFactoryCombinedStatsTest {

    private final ItemFactory factory = new ItemFactory(null, null, null, null);

    private ItemDefinition definitionWith(Map<String, Double> stats, java.util.List<UpgradeLevel> upgrades) {
        return new ItemDefinition("sword", null, Material.STICK, null, null, null, null, null, null, false, null,
                null, null, stats, null, null, null, null, null, null, null, null, null, null, upgrades, null, 0, 0,
                null);
    }

    @Test
    void combinedStatsAtLevelZeroReturnsOnlyBaseStats() {
        ItemDefinition definition = definitionWith(Map.of("damage", 5.0),
                java.util.List.of(new UpgradeLevel(1, Map.of("damage", 2.0), null, null, 0, null, 0)));

        Map<String, Double> combined = factory.combinedStats(definition, 0);

        assertEquals(5.0, combined.get("damage"));
    }

    @Test
    void combinedStatsAddsBonusesUpToAndIncludingCurrentLevel() {
        ItemDefinition definition = definitionWith(Map.of("damage", 5.0),
                java.util.List.of(
                        new UpgradeLevel(1, Map.of("damage", 2.0), null, null, 0, null, 0),
                        new UpgradeLevel(2, Map.of("damage", 3.0), null, null, 0, null, 0),
                        new UpgradeLevel(3, Map.of("damage", 100.0), null, null, 0, null, 0)));

        Map<String, Double> combined = factory.combinedStats(definition, 2);

        assertEquals(10.0, combined.get("damage"));
    }

    @Test
    void combinedStatsMergesNewStatKeysIntroducedByUpgrades() {
        ItemDefinition definition = definitionWith(Map.of("damage", 5.0),
                java.util.List.of(new UpgradeLevel(1, Map.of("crit_chance", 4.0), null, null, 0, null, 0)));

        Map<String, Double> combined = factory.combinedStats(definition, 1);

        assertEquals(5.0, combined.get("damage"));
        assertEquals(4.0, combined.get("crit_chance"));
    }
}
