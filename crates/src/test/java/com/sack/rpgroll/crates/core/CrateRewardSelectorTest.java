package com.sack.rpgroll.crates.core;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrateRewardSelectorTest {

    private CrateReward reward(String id, double weight) {
        return new CrateReward(id, id, Material.PAPER, List.of(), weight, false, List.of());
    }

    @Test
    void singleRewardIsAlwaysSelectedRegardlessOfWeight() {
        CrateReward only = reward("only", 5.0);

        for (int i = 0; i < 200; i++) {
            assertEquals(only, CrateRewardSelector.select(List.of(only)));
        }
    }

    @Test
    void everyRewardInTheListIsReachableOverManyRolls() {
        CrateReward common = reward("common", 70);
        CrateReward rare = reward("rare", 25);
        CrateReward legendary = reward("legendary", 5);
        List<CrateReward> rewards = List.of(common, rare, legendary);

        Map<String, Integer> counts = new java.util.HashMap<>();
        int iterations = 50_000;

        for (int i = 0; i < iterations; i++) {
            CrateReward selected = CrateRewardSelector.select(rewards);
            assertNotNull(selected);
            counts.merge(selected.id(), 1, Integer::sum);
        }

        assertEquals(iterations, counts.values().stream().mapToInt(Integer::intValue).sum());
        assertTrue(counts.containsKey("common"));
        assertTrue(counts.containsKey("rare"));
        assertTrue(counts.containsKey("legendary"));

        // 70/25/5 out of 100 total weight — allow generous tolerance since this is a live RNG,
        // just confirming rough proportionality, not exact percentages.
        double commonRatio = counts.get("common") / (double) iterations;
        double legendaryRatio = counts.get("legendary") / (double) iterations;

        assertTrue(commonRatio > 0.5, "common should be selected roughly 70% of the time, was " + commonRatio);
        assertTrue(legendaryRatio < 0.15, "legendary should be selected roughly 5% of the time, was " + legendaryRatio);
        assertTrue(counts.get("common") > counts.get("rare"));
        assertTrue(counts.get("rare") > counts.get("legendary"));
    }

    @Test
    void selectionAlwaysReturnsOneOfTheProvidedRewards() {
        List<CrateReward> rewards = List.of(reward("a", 1), reward("b", 1), reward("c", 1));

        for (int i = 0; i < 500; i++) {
            assertTrue(rewards.contains(CrateRewardSelector.select(rewards)));
        }
    }
}
