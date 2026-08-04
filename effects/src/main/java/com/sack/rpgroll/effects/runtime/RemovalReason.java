package com.sack.rpgroll.effects.runtime;

/** Por qué se removió un {@link ActiveEffect} — determina qué trigger de componentes dispara. */
public enum RemovalReason {
    /** Se le acabó la duración de forma natural — dispara componentes ON_EXPIRE. */
    EXPIRED,
    /** Se removió antes de tiempo (comando, API, dispel, conflicto, upgrade) — dispara ON_REMOVE. */
    MANUAL,
    /** Otro efecto en conflicto lo desplazó — dispara ON_REMOVE. */
    CONFLICT,
    /** Llegó al tope de stacks y se reemplazó por otro efecto — dispara ON_REMOVE. */
    UPGRADED
}
