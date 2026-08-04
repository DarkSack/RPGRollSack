package com.sack.rpgroll.fishing.core;

/** Franja horaria requerida, derivada de {@code World#getTime()} (0-24000). */
public enum TimeRequirement {
    DAY,
    NIGHT,
    DAWN,
    DUSK,
    NOON,
    MIDNIGHT
}
