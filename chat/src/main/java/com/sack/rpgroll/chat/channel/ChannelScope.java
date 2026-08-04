package com.sack.rpgroll.chat.channel;

/**
 * Determina cómo se resuelven los destinatarios de un canal.
 * Party/Raid/Dungeon del spec se cubren con {@link #TEAM} — ya representan
 * el mismo concepto de agrupación temporal que RPGRoll-Guilds modela como
 * Team; no se duplican como scopes propios.
 */
public enum ChannelScope {
    /** A todo el servidor, todos los mundos. */
    GLOBAL,
    /** Solo a jugadores dentro de {@code distance} bloques, mismo mundo. */
    PROXIMITY,
    /** A todo el mundo actual del emisor, sin límite de distancia. */
    WORLD,
    /** A los miembros de la guild del emisor (requiere RPGRoll-Guilds). */
    GUILD,
    /** A los miembros del equipo temporal del emisor (requiere RPGRoll-Guilds). */
    TEAM,
    /** A todo el que tenga el permiso del canal, sin importar mundo/distancia. */
    STAFF
}
