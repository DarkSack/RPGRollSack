package com.sack.rpgroll.tab.context;

/**
 * Tipos de condición "nativos" (resueltos directo contra la API de Bukkit,
 * sin depender de ningún addon). Cualquier otro concepto — guild, party,
 * dungeon, quest, region, combate, evento, temporada — se expresa con
 * {@link #PLACEHOLDER}: RPGRoll-TAB no sabe qué es una "guild", solo sabe
 * comparar el valor de un placeholder que otro addon registró.
 */
public enum ContextConditionType {
    WORLD,
    PERMISSION,
    GAMEMODE,
    DIMENSION,
    WEATHER,
    PLACEHOLDER
}
