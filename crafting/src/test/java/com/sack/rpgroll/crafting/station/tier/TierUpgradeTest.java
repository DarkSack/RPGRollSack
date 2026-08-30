package com.sack.rpgroll.crafting.station.tier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TierUpgradeTest {

    @Test
    void constructorClampsTierToAtLeastTwo() {
        assertEquals(2, new TierUpgrade(1, null, 0, null).tier());
        assertEquals(2, new TierUpgrade(0, null, 0, null).tier());
        assertEquals(3, new TierUpgrade(3, null, 0, null).tier());
    }

    @Test
    void constructorDefaultsNullCostToEmptyList() {
        assertTrue(new TierUpgrade(2, null, 0, null).cost().isEmpty());
    }

    @Test
    void constructorClampsNegativeEconomyCostToZero() {
        assertEquals(0.0, new TierUpgrade(2, null, -50, null).economyCost());
    }
}
