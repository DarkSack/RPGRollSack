package com.sack.rpgroll.items.core;

/**
 * Efecto de poción que el ítem aplica mientras está equipado (permanente
 * mientras se lo lleve puesto/en mano, según {@code requiresHeld}).
 */
public record ItemEffectDef(String potion, int amplifier, boolean requiresHeld) {
}
