package com.sack.rpgroll.fishing.core;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishSpeciesTest {

    private FishSpecies species(double minWeight, double maxWeight, String requiredBaitId) {
        return new FishSpecies("cod", null, null, 0, null, FishCategory.FRESHWATER, FishRarity.COMMON, Set.of(),
                Set.of(), Set.of(), minWeight, maxWeight, 1, 2, 10, 5, FishBehaviorType.SLOW, Set.of(), Set.of(),
                Set.of(), Set.of(), false, 0, false, requiredBaitId, null, null);
    }

    @Test
    void minWeightCannotBeZeroOrNegative() {
        FishSpecies species = species(-5, 10, null);
        assertEquals(0.01, species.minWeight());
    }

    @Test
    void maxWeightCannotBeBelowMinWeight() {
        FishSpecies species = species(5, 1, null);
        assertEquals(5.0, species.maxWeight());
    }

    @Test
    void hasRequiredBaitIsFalseWhenBlankOrNull() {
        assertFalse(species(1, 2, null).hasRequiredBait());
        assertFalse(species(1, 2, "  ").hasRequiredBait());
        assertTrue(species(1, 2, "worm").hasRequiredBait());
    }

    @Test
    void categoryAndRarityAndBehaviorDefaultWhenNull() {
        FishSpecies species = new FishSpecies("cod", null, null, 0, null, null, null, Set.of(), Set.of(), Set.of(),
                1, 2, 1, 2, 10, 5, null, Set.of(), Set.of(), Set.of(), Set.of(), false, 0, false, null, null, null);

        assertEquals(FishCategory.FRESHWATER, species.category());
        assertEquals(FishRarity.COMMON, species.rarity());
        assertEquals(FishBehaviorType.SLOW, species.behavior());
    }

    @Test
    void requiredLevelCannotBeNegative() {
        FishSpecies species = new FishSpecies("cod", null, null, 0, null, null, null, Set.of(), Set.of(), Set.of(),
                1, 2, 1, 2, 10, 5, null, Set.of(), Set.of(), Set.of(), Set.of(), true, -10, false, null, null, null);

        assertEquals(0, species.requiredLevel());
    }

}
