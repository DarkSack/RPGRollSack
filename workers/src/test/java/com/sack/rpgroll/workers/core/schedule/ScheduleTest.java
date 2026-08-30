package com.sack.rpgroll.workers.core.schedule;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScheduleTest {

    @Test
    void activityAtIsFreeWhenScheduleHasNoEntries() {
        Schedule schedule = new Schedule("empty", null, null, List.of());
        assertEquals(ScheduleActivity.FREE, schedule.activityAt(5000));
    }

    @Test
    void activityAtPicksLastEntryStartingAtOrBeforeTheTick() {
        Schedule schedule = new Schedule("day", null, null, List.of(
                new ScheduleEntry(0, ScheduleActivity.WAKE),
                new ScheduleEntry(2000, ScheduleActivity.WORK),
                new ScheduleEntry(12000, ScheduleActivity.EAT),
                new ScheduleEntry(13000, ScheduleActivity.SLEEP)));

        assertEquals(ScheduleActivity.WAKE, schedule.activityAt(0));
        assertEquals(ScheduleActivity.WORK, schedule.activityAt(2000));
        assertEquals(ScheduleActivity.WORK, schedule.activityAt(11999));
        assertEquals(ScheduleActivity.EAT, schedule.activityAt(12000));
        assertEquals(ScheduleActivity.SLEEP, schedule.activityAt(13000));
    }

    @Test
    void activityAtBeforeFirstEntryWrapsToTheLastEntryOfThePreviousDay() {
        Schedule schedule = new Schedule("day", null, null, List.of(
                new ScheduleEntry(2000, ScheduleActivity.WORK),
                new ScheduleEntry(13000, ScheduleActivity.SLEEP)));

        assertEquals(ScheduleActivity.SLEEP, schedule.activityAt(1000));
    }

    @Test
    void activityAtNormalizesTicksBeyondOneDay() {
        Schedule schedule = new Schedule("day", null, null, List.of(
                new ScheduleEntry(0, ScheduleActivity.WAKE),
                new ScheduleEntry(13000, ScheduleActivity.SLEEP)));

        assertEquals(ScheduleActivity.SLEEP, schedule.activityAt(24000 + 13500));
    }

    @Test
    void activityAtNormalizesNegativeTicks() {
        Schedule schedule = new Schedule("day", null, null, List.of(
                new ScheduleEntry(0, ScheduleActivity.WAKE),
                new ScheduleEntry(13000, ScheduleActivity.SLEEP)));

        assertEquals(ScheduleActivity.SLEEP, schedule.activityAt(-10000));
    }

    @Test
    void entryStartTickIsNormalizedIntoValidDayRange() {
        ScheduleEntry entry = new ScheduleEntry(-1000, ScheduleActivity.SLEEP);
        assertEquals(23000, entry.startTick());
    }

}
