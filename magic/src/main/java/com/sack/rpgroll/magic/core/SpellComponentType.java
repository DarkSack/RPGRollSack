package com.sack.rpgroll.magic.core;

/**
 * Un paso dentro del pipeline de un {@link Spell} — no existen hechizos
 * "programados", todos se arman encadenando estos mismos tipos en el orden
 * que quieras, igual que {@code EffectComponent} en RPGRoll-Effects.
 * {@link com.sack.rpgroll.magic.engine.SpellCastEngine} los ejecuta en
 * secuencia; los de movimiento (PROJECTILE/DASH/LEAP/ORBIT) corren sobre
 * varios ticks antes de dejar continuar al siguiente componente.
 */
public enum SpellComponentType {

    // ============ Movimiento ============
    /** Spawnea un proyectil que avanza hasta chocar con un bloque o entidad. */
    PROJECTILE,
    /** Empuja al caster en la dirección que mira una distancia fija, casi instantáneo. */
    DASH,
    /** Teletransporta al caster (a su punto de mira, o a un target). */
    TELEPORT,
    /** Salto parabólico en la dirección que mira el caster. */
    LEAP,
    /** El caster (o un punto) genera partículas orbitando a su alrededor por un tiempo. */
    ORBIT,

    // ============ Daño ============
    /** Daño directo a un target puntual (ej. el que colisionó un proyectil). */
    DAMAGE_DIRECT,
    /** Daño a todas las entidades vivas dentro de un radio. */
    DAMAGE_AREA,
    /** Daño a las entidades a lo largo de una línea recta desde el caster. */
    DAMAGE_LINE,
    /** Salta al enemigo vivo más cercano no golpeado todavía, hasta un máximo de saltos. */
    DAMAGE_CHAIN,
    /** Daño a las entidades dentro de un cono en la dirección que mira el caster. */
    DAMAGE_CONE,

    // ============ Visual (delegan en RPGRoll-Particles si está instalado) ============
    /** Partícula vanilla suelta, sin necesitar RPGRoll-Particles. */
    PARTICLE,
    /** Sonido vanilla suelto, sin necesitar RPGRoll-Particles. */
    SOUND,
    /** Dispara un efecto completo de RPGRoll-Particles por id (partículas+sonido+título+bossbar combinados). */
    VISUAL,

    // ============ Mundo ============
    /** Rompe el bloque en la posición actual del pipeline (proyectil/target). */
    BREAK_BLOCK,
    /** Coloca un bloque en la posición actual del pipeline. */
    PLACE_BLOCK,
    /** Prende fuego al target o al bloque bajo la posición actual. */
    IGNITE,
    /** Congela agua en un radio alrededor de la posición actual (la vuelve hielo temporalmente). */
    FREEZE_WATER,

    // ============ Entidades ============
    /** Invoca un mob vanilla en la posición actual (integración con RPGRoll-Mobs pendiente). */
    SUMMON,
    /** Cura al target (o al caster). */
    HEAL,
    /** Aplica un efecto de RPGRoll-Effects por id al target. */
    APPLY_EFFECT,
    /** Remueve un efecto de RPGRoll-Effects por id del target. */
    REMOVE_EFFECT,
    /** Empuja al target en la dirección que mira el caster. */
    PUSH,
    /** Atrae al target hacia el caster. */
    PULL,

    // ============ Control ============
    /** Espera N ticks antes de continuar con el siguiente componente. */
    DELAY,
    /** Mensaje de chat/actionbar al caster. */
    MESSAGE,
    /** Ejecuta un comando de consola con placeholders %caster%/%target%. */
    COMMAND
}
