package com.sack.rpgroll.crafting.condition;

public enum ConditionType {
    /** value ignorado, minValue = nivel mínimo del jugador. */
    LEVEL_MIN,
    /** value = id de raza requerida. */
    RACE,
    /** value = id de clase requerida. */
    CLASS,
    /** value = id de job, minValue = nivel mínimo en ese job. */
    JOB_MIN,
    /** value = nodo de permiso requerido. */
    PERMISSION,
    /** value = nombre del mundo requerido. */
    WORLD,
    /** value = "inicio-fin" en horas de reloj de Minecraft (0-24). */
    HOUR_RANGE,
    /** value = nombre de bioma vanilla requerido. */
    BIOME,
    /** value = id de estación (RPGRoll-Seasons) requerida. */
    SEASON,
    /** sin value: el jugador debe pertenecer a un gremio (RPGRoll-Guilds). */
    GUILD_MEMBER,
    /** value = CLEAR | RAIN | THUNDER. */
    WEATHER
}
