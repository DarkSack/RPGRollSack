package com.sack.rpgroll.fishing.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchQualityTest {

    @Test
    void priceMultiplierStrictlyIncreasesWithQuality() {
        CatchQuality[] ordered = { CatchQuality.COMMON, CatchQuality.GOOD, CatchQuality.EXCELLENT,
                CatchQuality.PERFECT, CatchQuality.MASTERWORK };

        for (int i = 1; i < ordered.length; i++) {
            assertTrue(ordered[i].priceMultiplier() > ordered[i - 1].priceMultiplier(),
                    ordered[i] + " should multiply price more than " + ordered[i - 1]);
        }
    }

}
