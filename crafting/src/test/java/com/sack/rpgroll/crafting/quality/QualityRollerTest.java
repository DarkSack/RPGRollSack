package com.sack.rpgroll.crafting.quality;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QualityRollerTest {

    private QualityRoller rollerWithFixedRandom(double randomValue) {
        return new QualityRoller(new Random() {
            @Override
            public double nextDouble() {
                return randomValue;
            }
        });
    }

    @Test
    void rollWithZeroSkillAndZeroRandomIsRough() {
        QualityRoller roller = rollerWithFixedRandom(0.0);

        assertEquals(CraftQuality.ROUGH, roller.roll(0.0));
    }

    @Test
    void rollWithMaxSkillAndMaxRandomIsLegendary() {
        QualityRoller roller = rollerWithFixedRandom(1.0);

        assertEquals(CraftQuality.LEGENDARY, roller.roll(1.0));
    }

    @Test
    void rollClampsSkillFactorAboveOne() {
        QualityRoller roller = rollerWithFixedRandom(1.0);

        assertEquals(CraftQuality.LEGENDARY, roller.roll(5.0));
    }

    @Test
    void rollClampsSkillFactorBelowZero() {
        QualityRoller roller = rollerWithFixedRandom(0.0);

        assertEquals(CraftQuality.ROUGH, roller.roll(-5.0));
    }

    @Test
    void rollWeightsSkillSeventyPercentAndRandomThirtyPercent() {
        QualityRoller roller = rollerWithFixedRandom(0.5);

        // score = (1.0 * 0.7 + 0.5 * 0.3) * 100 = 85 -> MASTERWORK
        assertEquals(CraftQuality.MASTERWORK, roller.roll(1.0));
    }
}
