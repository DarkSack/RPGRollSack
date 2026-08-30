package com.sack.rpgroll.seasons.runtime;

import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.DurationUnit;
import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.SeasonCalendar;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SubSeason;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeasonClockManagerTest {

    @Mock
    private SeasonManager seasonManager;

    @Mock
    private CalendarManager calendarManager;

    private SeasonClockManager clockManager;

    private Season season(String id, int durationDays, List<SubSeason> subSeasons) {
        return new Season(id, null, null, null, null, durationDays, DurationUnit.MINECRAFT_DAYS, null, subSeasons,
                Map.of(), Set.of(), List.of(), null, List.of(), 0, Set.of());
    }

    @BeforeEach
    void setUp() {
        clockManager = new SeasonClockManager(seasonManager, calendarManager);
    }

    @Test
    void getOrCreateReturnsTheSameInstanceForTheSameKey() {
        CalendarState first = clockManager.getOrCreate("world:overworld", "default");
        CalendarState second = clockManager.getOrCreate("world:overworld", "default");

        assertEquals(first, second);
    }

    @Test
    void tickDoesNothingWhenCalendarIsUnknown() {
        when(calendarManager.get("default")).thenReturn(Optional.empty());

        CalendarState state = clockManager.getOrCreate("world:overworld", "default");
        clockManager.tick(24000);

        assertEquals(0, state.seasonIndex());
        assertEquals(0, state.elapsedTicks());
    }

    @Test
    void tickAccumulatesTicksWithoutAdvancingBeforeSeasonEnds() {
        SeasonCalendar calendar = new SeasonCalendar("default", null, null, List.of("winter", "spring"));
        Season winter = season("winter", 10, List.of());

        lenient().when(calendarManager.get("default")).thenReturn(Optional.of(calendar));
        lenient().when(seasonManager.get("winter")).thenReturn(Optional.of(winter));

        CalendarState state = clockManager.getOrCreate("world:overworld", "default");
        clockManager.tick(24000 * 5);

        assertEquals(0, state.seasonIndex());
        assertEquals(24000 * 5, state.elapsedTicks());
    }

    @Test
    void tickAdvancesToNextSeasonWhenDurationIsReached() {
        SeasonCalendar calendar = new SeasonCalendar("default", null, null, List.of("winter", "spring"));
        Season winter = season("winter", 10, List.of());
        Season spring = season("spring", 10, List.of());

        when(calendarManager.get("default")).thenReturn(Optional.of(calendar));
        when(seasonManager.get("winter")).thenReturn(Optional.of(winter));
        when(seasonManager.get("spring")).thenReturn(Optional.of(spring));

        CalendarState state = clockManager.getOrCreate("world:overworld", "default");
        clockManager.tick(24000 * 10);

        assertEquals(1, state.seasonIndex());
        assertEquals(0, state.elapsedTicks());
        assertEquals(1, state.year());
    }

    @Test
    void tickWrapsToFirstSeasonAndIncrementsYearAfterTheLastSeason() {
        SeasonCalendar calendar = new SeasonCalendar("default", null, null, List.of("winter", "spring"));
        Season winter = season("winter", 10, List.of());
        Season spring = season("spring", 10, List.of());

        when(calendarManager.get("default")).thenReturn(Optional.of(calendar));
        when(seasonManager.get("winter")).thenReturn(Optional.of(winter));
        when(seasonManager.get("spring")).thenReturn(Optional.of(spring));

        CalendarState state = clockManager.getOrCreate("world:overworld", "default");
        clockManager.tick(24000 * 10);
        clockManager.tick(24000 * 10);

        assertEquals(0, state.seasonIndex());
        assertEquals(2, state.year());
    }

    @Test
    void tickAdvancesThroughSubSeasonsBeforeAdvancingTheParentSeason() {
        SubSeason earlyWinter = new SubSeason("early", null, 5, DurationUnit.MINECRAFT_DAYS, null);
        SubSeason lateWinter = new SubSeason("late", null, 5, DurationUnit.MINECRAFT_DAYS, null);
        SeasonCalendar calendar = new SeasonCalendar("default", null, null, List.of("winter", "spring"));
        Season winter = season("winter", 10, List.of(earlyWinter, lateWinter));
        Season spring = season("spring", 10, List.of());

        when(calendarManager.get("default")).thenReturn(Optional.of(calendar));
        when(seasonManager.get("winter")).thenReturn(Optional.of(winter));
        lenient().when(seasonManager.get("spring")).thenReturn(Optional.of(spring));

        CalendarState state = clockManager.getOrCreate("world:overworld", "default");
        clockManager.tick(24000 * 5);

        assertEquals(0, state.seasonIndex());
        assertEquals(0, state.subSeasonIndex());
        assertEquals(0, state.elapsedTicks());
    }

    @Test
    void resolveCurrentSeasonReturnsEmptyForUnknownClockKey() {
        assertTrue(clockManager.resolveCurrentSeason("nonexistent").isEmpty());
    }

    @Test
    void resolveCurrentSeasonUsesFloorModForOutOfRangeSeasonIndex() {
        SeasonCalendar calendar = new SeasonCalendar("default", null, null, List.of("winter", "spring"));
        Season spring = season("spring", 10, List.of());

        when(calendarManager.get("default")).thenReturn(Optional.of(calendar));
        when(seasonManager.get("spring")).thenReturn(Optional.of(spring));

        CalendarState state = clockManager.getOrCreate("world:overworld", "default");
        state.setSeasonIndex(3); // floorMod(3, 2) == 1 -> "spring"

        Optional<Season> resolved = clockManager.resolveCurrentSeason(state);

        assertTrue(resolved.isPresent());
        assertEquals("spring", resolved.get().id());
    }

    @Test
    void resolveCurrentSubSeasonIsEmptyWhenSeasonHasNoSubSeasons() {
        SeasonCalendar calendar = new SeasonCalendar("default", null, null, List.of("winter"));
        Season winter = season("winter", 10, List.of());

        when(calendarManager.get("default")).thenReturn(Optional.of(calendar));
        when(seasonManager.get("winter")).thenReturn(Optional.of(winter));

        CalendarState state = clockManager.getOrCreate("world:overworld", "default");

        assertFalse(clockManager.resolveCurrentSubSeason(state).isPresent());
    }

    @Test
    void restoreReplacesTheClockUnderItsOwnKey() {
        CalendarState restored = new CalendarState("world:overworld", "default");
        restored.restoreExact(1, -1, 500, 3, -1);

        clockManager.restore(restored);

        assertEquals(restored, clockManager.get("world:overworld").orElseThrow());
        assertEquals(1, clockManager.allClocks().size());
    }

}
