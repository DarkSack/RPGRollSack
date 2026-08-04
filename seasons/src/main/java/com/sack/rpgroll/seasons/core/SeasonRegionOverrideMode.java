package com.sack.rpgroll.seasons.core;

public enum SeasonRegionOverrideMode {
    /** Usa el calendario asignado al mundo — el comportamiento por defecto (ninguna región lo necesita explícitamente). */
    FOLLOW_WORLD_CALENDAR,
    /** Siempre la misma estación fija, sin reloj propio (ej. "Desierto: siempre verano"). */
    PINNED_SEASON,
    /** Corre su propio calendario con un reloj independiente al del mundo. */
    PINNED_CALENDAR
}
