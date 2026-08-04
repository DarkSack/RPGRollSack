package com.sack.rpgroll.seasons.core;

/**
 * Qué hace un paso de un {@link WorldEvent} — se ejecutan en secuencia
 * sobre TODOS los jugadores online del mundo donde se disparó (no hay
 * noción de "target" individual, a diferencia de RPGRoll-Magic/Effects).
 */
public enum WorldEventComponentType {
    /** Partícula vanilla sobre cada jugador. */
    PARTICLE,
    /** Sonido vanilla para cada jugador. */
    SOUND,
    /** Efecto completo de SackEffects por id, sobre cada jugador. */
    VISUAL,
    /** Aplica un efecto de RPGRoll-Effects por id a cada jugador. */
    APPLY_EFFECT,
    /** Invoca un mob de RPGRoll-Mobs cerca de cada jugador. */
    SPAWN_MOB,
    /** Mensaje de chat a todo el mundo. */
    MESSAGE,
    /** Comando de consola, una vez (no por jugador) con placeholder %world%. */
    COMMAND,
    /** Fuerza clima (tormenta/despejado) en el mundo mientras dura el evento. */
    SET_WEATHER
}
