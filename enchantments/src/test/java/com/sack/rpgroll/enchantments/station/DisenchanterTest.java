package com.sack.rpgroll.enchantments.station;

import com.sack.rpgroll.enchantments.core.Rarity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisenchanterTest {

    @Test
    void priceGrowsWithTheLevelAndDoublesForTreasureOrCurses() {
        assertEquals(1500, Disenchanter.price(1500, 1, false));
        assertEquals(4500, Disenchanter.price(1500, 3, false));
        assertEquals(9000, Disenchanter.price(1500, 3, true));
        assertEquals(1, Disenchanter.price(1, 1, false));
        assertEquals(4, Disenchanter.price(1, 2, true));
    }

    @Test
    void levelZeroOrLessStillCostsOneLevel() {
        assertEquals(1500, Disenchanter.price(1500, 0, false));
    }

    @Test
    void legendaryAndAboveCountAsTreasure() {
        assertFalse(Disenchanter.premium(Rarity.EPIC));
        assertTrue(Disenchanter.premium(Rarity.LEGENDARY));
        assertTrue(Disenchanter.premium(Rarity.DIVINE));
    }
}
