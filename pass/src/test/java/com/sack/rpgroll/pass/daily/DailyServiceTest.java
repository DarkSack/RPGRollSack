package com.sack.rpgroll.pass.daily;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DailyServiceTest {

    private static final long TODAY = 20_000;

    @Test
    void consecutiveDaysGrowTheStreak() {
        assertEquals(5, DailyService.nextStreak(TODAY - 1, TODAY, 4, true));
    }

    @Test
    void missingADayResetsOnlyWhenConfigured() {
        assertEquals(1, DailyService.nextStreak(TODAY - 3, TODAY, 4, true));
        assertEquals(5, DailyService.nextStreak(TODAY - 3, TODAY, 4, false));
    }

    @Test
    void firstEverClaimStartsAtOne() {
        assertEquals(1, DailyService.nextStreak(Long.MIN_VALUE, TODAY, 0, true));
    }

    @Test
    void claimingTwiceTheSameDayDoesNotMoveIt() {
        assertEquals(4, DailyService.nextStreak(TODAY, TODAY, 4, true));
    }

}
