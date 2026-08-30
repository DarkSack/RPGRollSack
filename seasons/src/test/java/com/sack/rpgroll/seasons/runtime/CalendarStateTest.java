package com.sack.rpgroll.seasons.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalendarStateTest {

    @Test
    void newStateStartsAtSeasonZeroYearOne() {
        CalendarState state = new CalendarState("world:overworld", "default");

        assertEquals(0, state.seasonIndex());
        assertEquals(-1, state.subSeasonIndex());
        assertEquals(0, state.elapsedTicks());
        assertEquals(1, state.year());
    }

    @Test
    void advanceSeasonIncrementsIndexAndResetsTicks() {
        CalendarState state = new CalendarState("world:overworld", "default");
        state.addElapsedTicks(5000);

        state.advanceSeason(4);

        assertEquals(1, state.seasonIndex());
        assertEquals(0, state.elapsedTicks());
        assertEquals(1, state.year());
    }

    @Test
    void advanceSeasonWrapsToZeroAndIncrementsYearAtEndOfCalendar() {
        CalendarState state = new CalendarState("world:overworld", "default");
        state.setSeasonIndex(3);

        state.advanceSeason(4);

        assertEquals(0, state.seasonIndex());
        assertEquals(2, state.year());
    }

    @Test
    void advanceSubSeasonIncrementsIndexAndResetsElapsedTicks() {
        CalendarState state = new CalendarState("world:overworld", "default");
        state.addElapsedTicks(500);

        state.advanceSubSeason();

        assertEquals(0, state.subSeasonIndex());
        assertEquals(0, state.elapsedTicks());
    }

    @Test
    void setCalendarIdResetsSeasonProgressButNotYear() {
        CalendarState state = new CalendarState("world:overworld", "default");
        state.advanceSeason(4);
        state.advanceSeason(4);

        state.setCalendarId("alt-calendar");

        assertEquals("alt-calendar", state.calendarId());
        assertEquals(0, state.seasonIndex());
        assertEquals(-1, state.subSeasonIndex());
        assertEquals(0, state.elapsedTicks());
        assertEquals(1, state.year());
    }

    @Test
    void restoreExactSetsAllFieldsWithoutTriggeringResets() {
        CalendarState state = new CalendarState("world:overworld", "default");

        state.restoreExact(2, 1, 12345, 5, 42);

        assertEquals(2, state.seasonIndex());
        assertEquals(1, state.subSeasonIndex());
        assertEquals(12345, state.elapsedTicks());
        assertEquals(5, state.year());
        assertEquals(42, state.lastWorldEventDayRoll());
    }

}
