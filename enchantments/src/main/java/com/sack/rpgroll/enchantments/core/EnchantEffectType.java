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
    PICKUP_ITEMS,

    // ---- herramientas (trigger BLOCK_BREAK o BLOCK_INTERACT, ítem en la mano principal)
    /** Rompe la veta entera de la mena golpeada. */
    VEIN_MINE,
    /** Tala el árbol entero y deshace sus hojas. */
    TREE_FELL,
    /** Rompe un cuadrado alrededor del bloque, de cara al jugador (martillo). */
    AREA_MINE,
    /** Lo que suelta el bloque sale ya fundido. */
    AUTO_SMELT,
    /** Labra un cuadrado de tierra, o cosecha y replanta un cuadrado de cultivos maduros. */
    TILL_AREA,

    // ---- equipo
    /** Repara con la experiencia recogida (trigger EXP_PICKUP). */
    REPAIR,
    /** Daño extra sobre el golpe, escalado por la carga del ataque. */
    DAMAGE_BONUS,
    /** Empuja al objetivo lejos del jugador. */
    KNOCKBACK,
    /** Devuelve el proyectil parado con el escudo a quien lo disparó. */
    REFLECT,
    /** Acorta el tiempo que un hacha deja el escudo desactivado (trigger SHIELD_DISABLE). */
    SHIELD_COOLDOWN
}
