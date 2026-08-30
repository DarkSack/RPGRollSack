package com.sack.rpgroll.seasons.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonRegionTest {

    @Test
    void containsHandlesReversedMinMaxCoordinates() {
        SeasonRegion region = new SeasonRegion("polar-cap", "world", 10, 10, 10, -10, -10, -10,
                SeasonRegionOverrideMode.PINNED_SEASON, "winter", null);

        assertTrue(region.contains("world", 0, 0, 0));
        assertTrue(region.contains("WORLD", -10, -10, -10));
        assertFalse(region.contains("world", 11, 0, 0));
    }

    @Test
    void containsIsFalseForDifferentWorld() {
        SeasonRegion region = new SeasonRegion("polar-cap", "world", 0, 0, 0, 10, 10, 10,
                SeasonRegionOverrideMode.FOLLOW_WORLD_CALENDAR, null, null);

        assertFalse(region.contains("other_world", 5, 5, 5));
    }

    @Test
    void clockKeyIsPrefixedAndNeverCollidesWithWorldKeys() {
        SeasonRegion region = new SeasonRegion("polar-cap", "world", 0, 0, 0, 10, 10, 10,
                SeasonRegionOverrideMode.PINNED_CALENDAR, null, "winter-only");

        assertEquals("region:polar-cap", region.clockKey());
    }

    @Test
    void overrideModeDefaultsToFollowWorldCalendarWhenNull() {
        SeasonRegion region = new SeasonRegion("polar-cap", "world", 0, 0, 0, 10, 10, 10, null, null, null);
        assertEquals(SeasonRegionOverrideMode.FOLLOW_WORLD_CALENDAR, region.overrideMode());
    }

    @Test
    void blankPinnedIdsBecomeNull() {
        SeasonRegion region = new SeasonRegion("polar-cap", "world", 0, 0, 0, 10, 10, 10,
                SeasonRegionOverrideMode.PINNED_SEASON, "  ", "  ");

        assertEquals(null, region.pinnedSeasonId());
        assertEquals(null, region.pinnedCalendarId());
    }

}
