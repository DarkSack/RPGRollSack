package com.sack.rpgroll.pass.mission;

/** Cada cuánto se renueva una misión. */
public enum MissionScope {
    /** Se sortean unas cuantas cada día. */
    DAILY,
    /** Se sortean unas cuantas cada semana (lunes). */
    WEEKLY,
    /** Fijas durante toda la temporada, se completan una vez. */
    SEASON
}
