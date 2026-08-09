package com.sack.rpgroll.workers.core.schedule;

/** @param startTick hora del día de Minecraft (0-24000, 0 = amanecer) en la que arranca esta actividad. */
public record ScheduleEntry(long startTick, ScheduleActivity activity) {

    public ScheduleEntry {
        startTick = ((startTick % 24000) + 24000) % 24000;
    }

}
