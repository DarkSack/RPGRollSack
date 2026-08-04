package com.sack.rpgroll.magic.core;

/**
 * Cómo dispara un {@link Spell} mientras el jugador sostiene un catalizador
 * válido con este hechizo seleccionado. Combos y teclas personalizadas
 * quedan para una próxima pasada — necesitarían un mod de cliente o un
 * sistema de keybinds propio que todavía no existe.
 */
public enum SpellCastTrigger {
    LEFT_CLICK,
    RIGHT_CLICK,
    /** Mantener click derecho durante {@code cast-time} ticks — se cancela si se suelta antes, si el caster se mueve o recibe daño. */
    HOLD
}
