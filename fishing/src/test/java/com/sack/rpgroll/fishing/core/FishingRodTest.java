package com.sack.rpgroll.fishing.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FishingRodTest {

    @ParameterizedTest
    @ValueSource(doubles = { -5, 0 })
    void nonPositiveMultipliersDefaultToOne(double nonPositive) {
        FishingRod rod = new FishingRod("basic", null, null, null, 1, nonPositive, nonPositive, 0, nonPositive,
                nonPositive, Set.of());

        assertEquals(1.0, rod.castPower());
        assertEquals(1.0, rod.reelSpeed());
        assertEquals(1.0, rod.resistance());
        assertEquals(1.0, rod.luckBonus());
    }

    @Test
    void precisionCanBeZeroButNotNegative() {
        FishingRod rod = new FishingRod("basic", null, null, null, 1, 1, 1, -5, 1, 1, Set.of());
        assertEquals(0, rod.precision());
    }

    @Test
    void durabilityIsAtLeastOne() {
        FishingRod rod = new FishingRod("basic", null, null, null, -10, 1, 1, 0, 1, 1, Set.of());
        assertEquals(1, rod.durability());
    }

    @Test
    void defaultRodHasNoBonusesAndVanillaMaterial() {
        FishingRod rod = FishingRod.defaultRod();

        assertEquals("FISHING_ROD", rod.material());
        assertEquals(1.0, rod.castPower());
        assertEquals(1.0, rod.reelSpeed());
        assertEquals(0.0, rod.precision());
        assertEquals(1.0, rod.resistance());
        assertEquals(1.0, rod.luckBonus());
    }

}
