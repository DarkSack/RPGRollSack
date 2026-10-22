package com.sack.rpgroll.worldforge.regen;

import java.util.EnumSet;
import java.util.Set;

/**
 * States of a regeneration or restore job and the only moves allowed between them
 * (ARCHITECTURE §Estados). The backup is taken right before the files are edited, when the
 * server no longer uses them: after unloading the world, or at startup before the worlds open.
 */
public enum JobState {
    PLANNED, CONFIRMED, PENDING_RESTART, BACKING_UP, READY, RUNNING,
    COMPLETED, FAILED, INTERRUPTED, NEEDS_RECOVERY, RESTORED, CANCELLED;

    public Set<JobState> next() {
        return switch (this) {
            case PLANNED -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(BACKING_UP, PENDING_RESTART, FAILED, CANCELLED);
            case PENDING_RESTART -> EnumSet.of(BACKING_UP, CANCELLED);
            case BACKING_UP -> EnumSet.of(READY, FAILED, INTERRUPTED);
            case READY -> EnumSet.of(RUNNING, CANCELLED);
            case RUNNING -> EnumSet.of(COMPLETED, FAILED, INTERRUPTED);
            case INTERRUPTED -> EnumSet.of(NEEDS_RECOVERY);
            case NEEDS_RECOVERY -> EnumSet.of(RESTORED, CANCELLED);
            case COMPLETED, FAILED, RESTORED, CANCELLED -> EnumSet.noneOf(JobState.class);
        };
    }

    public boolean canMoveTo(JobState to) {
        return next().contains(to);
    }

    public boolean finished() {
        return next().isEmpty();
    }

    /** While a job is in one of these states, its world accepts no other destructive operation. */
    public boolean locksWorld() {
        return !finished() && this != PLANNED;
    }
}
