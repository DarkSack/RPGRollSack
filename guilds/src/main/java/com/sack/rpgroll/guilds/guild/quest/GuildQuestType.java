package com.sack.rpgroll.guilds.guild.quest;

/**
 * Tipos de objetivo de quest de guild (spec: "derrotar jefes, completar
 * dungeons, conseguir recursos, ganar guerras, construir base"). El
 * progreso se acumula entre todos los miembros (objetivo compartido).
 * <p>
 * {@code WIN_WAR} existe para completitud del spec, pero no tiene ningún
 * gancho automático de progreso en esta pasada — no hay motor de guerra
 * (decisión de alcance). Una quest de este tipo nunca avanza sola.
 */
public enum GuildQuestType {
    DEFEAT_BOSS,
    COMPLETE_DUNGEON,
    GATHER_RESOURCE,
    WIN_WAR,
    BUILD_BASE
}
