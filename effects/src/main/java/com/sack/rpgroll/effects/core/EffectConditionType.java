package com.sack.rpgroll.effects.core;

/**
 * Requisito que se evalúa al intentar aplicar un efecto — si alguno falla,
 * el efecto no se aplica. Todas se resuelven contra {@code RPGRollAPI} (ya
 * una dependencia estable de este módulo) salvo GUILD/TEAM (vía
 * {@code GuildsAPI}, dependencia blanda). REGION y DUNGEON no están
 * disponibles todavía — Quests y Dungeons no exponen una API pública
 * estable para consultarlas desde otro addon (quedan para una próxima
 * pasada de integración).
 */
public enum EffectConditionType {
    LEVEL_MIN,
    LEVEL_MAX,
    RACE,
    CLASS,
    JOB,
    WORLD,
    WEATHER,
    TIME_RANGE,
    HEALTH_BELOW,
    HEALTH_ABOVE,
    MANA_BELOW,
    MANA_ABOVE,
    GUILD,
    TEAM,
    PERMISSION
}
