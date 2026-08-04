package com.sack.rpgroll.magic.core;

/**
 * Qué modifica una {@link Rune} sobre el hechizo al que está adjunta —
 * aplicado en tiempo de cast por {@code RuneModifierApplier} sobre una
 * copia de trabajo del pipeline, nunca sobre la definición guardada del
 * hechizo.
 */
public enum RuneModifierType {
    /** Multiplica cuántos PROJECTILE se spawnean, con un pequeño desvío de ángulo entre ellos (params: count). */
    EXTRA_PROJECTILES,
    /** Los componentes de daño no se detienen en el primer impacto — siguen hasta max-pierces objetivos (params: max-pierces). */
    PIERCING,
    /** Agrega un DAMAGE_AREA + PARTICLE de explosión justo después de la colisión (params: radius, damage). */
    EXPLOSIVE,
    /** Agrega una aplicación extra de un efecto de RPGRoll-Effects junto con el daño del hechizo (params: effect-id). */
    APPLY_EFFECT,
    /** Multiplica/ajusta el costo de maná del hechizo (params: multiplier). */
    COST_MODIFIER,
    /** Multiplica/ajusta el cooldown del hechizo (params: multiplier). */
    COOLDOWN_MODIFIER
}
