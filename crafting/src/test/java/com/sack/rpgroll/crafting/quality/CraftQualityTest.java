package com.sack.rpgroll.crafting.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CraftQualityTest {

    @ParameterizedTest
    @CsvSource({
            "0, ROUGH",
            "24.9, ROUGH",
            "25, STANDARD",
            "54.9, STANDARD",
            "55, FINE",
            "79.9, FINE",
            "80, MASTERWORK",
            "94.9, MASTERWORK",
            "95, LEGENDARY",
            "100, LEGENDARY",
    })
    void fromScoreMapsBoundariesToCorrectTier(double score, CraftQuality expected) {
        assertEquals(expected, CraftQuality.fromScore(score));
    }

    @Test
    void valueMultiplierIncreasesWithTier() {
        assertEquals(0.75, CraftQuality.ROUGH.valueMultiplier());
        assertEquals(1.0, CraftQuality.STANDARD.valueMultiplier());
        assertEquals(1.3, CraftQuality.FINE.valueMultiplier());
        assertEquals(1.7, CraftQuality.MASTERWORK.valueMultiplier());
        assertEquals(2.5, CraftQuality.LEGENDARY.valueMultiplier());
    }

    @Test
    void coloredLabelConcatenatesColorCodeAndDisplayName() {
        assertEquals(CraftQuality.LEGENDARY.colorCode() + CraftQuality.LEGENDARY.displayName(),
                CraftQuality.LEGENDARY.coloredLabel());
    }
}
