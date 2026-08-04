package com.sack.rpgroll.effects.core;

/**
 * Qué hace un {@link EffectComponent} cuando dispara. Un efecto combina
 * varios componentes — ej. Bleeding = PERIODIC_DAMAGE (ON_SECOND) +
 * PARTICLE (ON_SECOND) + SOUND (ON_APPLY).
 */
public enum EffectComponentType {
    /** Sube/baja un atributo vanilla (velocidad, daño de ataque, etc.) mientras el efecto esté activo. */
    ATTRIBUTE_MODIFIER,
    /** Daño periódico — el monto puede escalar con el amplifier/stacks. */
    PERIODIC_DAMAGE,
    /** Curación periódica. */
    PERIODIC_HEAL,
    /** Multiplica la velocidad de movimiento (independiente de ATTRIBUTE_MODIFIER, más simple de usar en YAML). */
    MOVEMENT_MODIFIER,
    /** Aplica un PotionEffect vanilla real (para que se vea el ícono clásico en el HUD de Minecraft). */
    POTION_VANILLA,
    /** Dispara un efecto de SackEffects (partículas/sonido/título/bossbar) por su id. */
    VISUAL,
    /** Sonido suelto sin necesitar un efecto de SackEffects declarado aparte. */
    SOUND,
    /** Mensaje de chat/actionbar al titular del efecto. */
    MESSAGE,
    /** Otorga absorción (vida de escudo) que se descuenta antes que la vida real. */
    SHIELD,
    /** Impide lanzar hechizos/habilidades mientras esté activo (otros addons deben consultar isSilenced). */
    SILENCE,
    /** Marca al titular como confundido (otros addons deciden qué hacer con eso, ej. invertir controles). */
    CONFUSION,
    /** Aplica este mismo efecto (u otro) a las entidades cercanas mientras esté activo. */
    AURA,
    /** Ejecuta un comando de consola con placeholders %player%/%target%. */
    COMMAND
}
