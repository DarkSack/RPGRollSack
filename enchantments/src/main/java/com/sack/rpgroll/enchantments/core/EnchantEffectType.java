package com.sack.rpgroll.enchantments.core;

/**
 * Tipo de efecto que un encantamiento puede ejecutar cuando su trigger,
 * probabilidad y condiciones se cumplen.
 */
public enum EnchantEffectType {
    DAMAGE,
    HEAL,
    LIGHTNING,
    POTION,
    FIRE,
    EXPLOSION,
    TELEPORT,
    COMMAND,
    MESSAGE,
    PARTICLE,
    /** Efecto completo de RPGRoll-FX por id (formas, sonidos y tiempos ya compuestos). */
    PARTICLES,
    SOUND,
    PICKUP_ITEMS
}
