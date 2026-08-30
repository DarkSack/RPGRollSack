package com.sack.rpgroll.crafting.proficiency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProficiencyLevelCurveTest {

    @Test
    void totalXpToReachLevelOneIsZero() {
        assertEquals(0, ProficiencyLevelCurve.totalXpToReach(1));
    }

    @Test
    void totalXpToReachClampsBelowLevelOne() {
        assertEquals(ProficiencyLevelCurve.totalXpToReach(1), ProficiencyLevelCurve.totalXpToReach(0));
        assertEquals(ProficiencyLevelCurve.totalXpToReach(1), ProficiencyLevelCurve.totalXpToReach(-10));
    }

    @Test
    void totalXpToReachGrowsWithLevel() {
        assertTrue(ProficiencyLevelCurve.totalXpToReach(5) > ProficiencyLevelCurve.totalXpToReach(2));
    }

    @Test
    void levelForZeroXpIsLevelOne() {
        assertEquals(1, ProficiencyLevelCurve.levelFor(0));
    }

    @Test
    void levelForMatchesTotalXpToReachRoundTrip() {
        double xpForLevelTen = ProficiencyLevelCurve.totalXpToReach(10);

        assertEquals(10, ProficiencyLevelCurve.levelFor(xpForLevelTen));
    }

    @Test
    void levelForNeverExceedsMaxLevel() {
        assertEquals(ProficiencyLevelCurve.MAX_LEVEL, ProficiencyLevelCurve.levelFor(Double.MAX_VALUE));
    }

    @Test
    void progressWithinLevelIsZeroAtLevelFloor() {
        double xpForLevelFive = ProficiencyLevelCurve.totalXpToReach(5);

        assertEquals(0.0, ProficiencyLevelCurve.progressWithinLevel(xpForLevelFive));
    }

    @Test
    void progressWithinLevelIsOneAtMaxLevel() {
        double xpForMax = ProficiencyLevelCurve.totalXpToReach(ProficiencyLevelCurve.MAX_LEVEL);

        assertEquals(1.0, ProficiencyLevelCurve.progressWithinLevel(xpForMax));
    }

    @Test
    void factorForIsOneAtMaxLevelAndLessBelow() {
        double xpForMax = ProficiencyLevelCurve.totalXpToReach(ProficiencyLevelCurve.MAX_LEVEL);

        assertEquals(1.0, ProficiencyLevelCurve.factorFor(xpForMax));
        assertTrue(ProficiencyLevelCurve.factorFor(0) < 1.0);
    }
}
