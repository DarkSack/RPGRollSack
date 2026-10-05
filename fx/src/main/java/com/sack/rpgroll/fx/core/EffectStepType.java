package com.sack.rpgroll.fx.core;

/** Qué hace un {@link EffectStep} — cada tipo interpreta sus propios params libres. */
public enum EffectStepType {
    PARTICLE,
    SOUND,
    TITLE,
    ACTIONBAR,
    BOSSBAR,
    POTION,
    /** Cohete que explota al instante con los colores del step, sin hacer daño. */
    FIREWORK,
    /** Rayo solo visual: sin fuego ni daño. */
    LIGHTNING
}
