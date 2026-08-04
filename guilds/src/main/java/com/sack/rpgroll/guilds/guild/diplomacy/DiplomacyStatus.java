package com.sack.rpgroll.guilds.guild.diplomacy;

/**
 * Estado diplomático entre dos guilds (spec: "Neutral/Aliado/En Guerra/En
 * Paz/En Conflicto"). {@code WAR} se registra únicamente como dato — no
 * existe (todavía) un motor de guerra con reglas/objetivos/recompensas;
 * queda documentado como punto de extensión futuro (ver GuildCommand,
 * sección diplomacia).
 */
public enum DiplomacyStatus {
    NEUTRAL,
    ALLIED,
    PEACE,
    CONFLICT,
    WAR
}
