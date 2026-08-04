package com.sack.rpgroll.effects.core;

/**
 * Cuándo dispara un {@link EffectComponent} de un efecto activo.
 * <p>
 * REGION_ENTER/REGION_EXIT/COMBAT_START/SPELL_CAST todavía no tienen un
 * listener propio dentro de este módulo (dependen de eventos de otros
 * addons — Quests para regiones, un futuro addon de Magic para hechizos).
 * Quedan declarados para que YAML/otros addons ya puedan escribirlos, y
 * cualquier addon puede dispararlos manualmente vía
 * {@code EffectsAPI.get().fireTrigger(...)} — se conectan a un listener real
 * en una próxima pasada de integración.
 */
public enum EffectTriggerType {
    ON_APPLY,
    ON_TICK,
    ON_SECOND,
    ON_DAMAGE_TAKEN,
    ON_DAMAGE_DEALT,
    ON_DEATH,
    ON_HEAL,
    ON_FOOD_CONSUME,
    ON_EXPIRE,
    ON_REMOVE,
    REGION_ENTER,
    REGION_EXIT,
    COMBAT_START,
    SPELL_CAST
}
