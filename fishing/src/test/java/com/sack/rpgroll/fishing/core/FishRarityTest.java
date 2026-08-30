package com.sack.rpgroll.fishing.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FishRarityTest {

    @Test
    void baseWeightStrictlyDecreasesAsRarityIncreases() {
        FishRarity[] ordered = { FishRarity.COMMON, FishRarity.UNCOMMON, FishRarity.RARE, FishRarity.EPIC,
                FishRarity.LEGENDARY };

        for (int i = 1; i < ordered.length; i++) {
            assertTrue(ordered[i].baseWeight() < ordered[i - 1].baseWeight(),
                    ordered[i] + " should be rarer (lower weight) than " + ordered[i - 1]);
        }
    }

}
