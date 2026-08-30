package com.sack.rpgroll.seasons.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationUnitTest {

    @Test
    void minecraftDaysConvertToTwentyFourThousandTicksEach() {
        assertEquals(24000L, DurationUnit.MINECRAFT_DAYS.toTicks(1));
        assertEquals(72000L, DurationUnit.MINECRAFT_DAYS.toTicks(3));
    }

    @Test
    void realHoursConvertUsingTwentyTicksPerSecond() {
        assertEquals(72000L, DurationUnit.REAL_HOURS.toTicks(1));
    }

    @Test
    void realDaysConvertUsingTwentyFourHours() {
        assertEquals(1_728_000L, DurationUnit.REAL_DAYS.toTicks(1));
    }

    @Test
    void realWeeksConvertUsingSevenDays() {
        assertEquals(12_096_000L, DurationUnit.REAL_WEEKS.toTicks(1));
    }

    @Test
    void zeroAmountAlwaysProducesZeroTicks() {
        for (DurationUnit unit : DurationUnit.values()) {
            assertEquals(0L, unit.toTicks(0));
        }
    }

}
