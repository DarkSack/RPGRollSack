package com.sack.rpgroll.seasons.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonTest {

    private Season season(int durationAmount, DurationUnit unit, String exclusiveBossId, double worldEventDailyChance) {
        return new Season("winter", null, null, null, null, durationAmount, unit, null, List.of(), Map.of(), Set.of(),
                List.of(), exclusiveBossId, List.of(), worldEventDailyChance, Set.of());
    }

    @Test
    void durationAmountCannotBeZeroOrNegative() {
        assertEquals(1, season(0, DurationUnit.MINECRAFT_DAYS, null, 0).durationAmount());
        assertEquals(1, season(-5, DurationUnit.MINECRAFT_DAYS, null, 0).durationAmount());
    }

    @Test
    void durationTicksDelegatesToDurationUnit() {
        Season season = season(3, DurationUnit.MINECRAFT_DAYS, null, 0);
        assertEquals(72000L, season.durationTicks());
    }

    @Test
    void worldEventDailyChanceIsClampedBetweenZeroAndOne() {
        assertEquals(0.0, season(1, DurationUnit.MINECRAFT_DAYS, null, -0.5).worldEventDailyChance());
        assertEquals(1.0, season(1, DurationUnit.MINECRAFT_DAYS, null, 1.5).worldEventDailyChance());
    }

    @Test
    void hasSubSeasonsIsFalseWhenListIsEmpty() {
        assertFalse(season(1, DurationUnit.MINECRAFT_DAYS, null, 0).hasSubSeasons());
    }

    @Test
    void hasSubSeasonsIsTrueWhenListIsNotEmpty() {
        Season season = new Season("winter", null, null, null, null, 1, DurationUnit.MINECRAFT_DAYS, null,
                List.of(new SubSeason("early", null, 1, DurationUnit.MINECRAFT_DAYS, null)), Map.of(), Set.of(),
                List.of(), null, List.of(), 0, Set.of());

        assertTrue(season.hasSubSeasons());
    }

    @Test
    void hasExclusiveBossIsFalseWhenBlank() {
        assertFalse(season(1, DurationUnit.MINECRAFT_DAYS, "  ", 0).hasExclusiveBoss());
        assertFalse(season(1, DurationUnit.MINECRAFT_DAYS, null, 0).hasExclusiveBoss());
    }

    @Test
    void hasExclusiveBossIsTrueWhenPresent() {
        assertTrue(season(1, DurationUnit.MINECRAFT_DAYS, "frost-titan", 0).hasExclusiveBoss());
    }

    @Test
    void durationUnitDefaultsToMinecraftDaysWhenNull() {
        Season season = season(1, null, null, 0);
        assertEquals(DurationUnit.MINECRAFT_DAYS, season.durationUnit());
    }

}
