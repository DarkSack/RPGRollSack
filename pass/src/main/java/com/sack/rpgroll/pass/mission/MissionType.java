package com.sack.rpgroll.pass.mission;

/** Qué cuenta para una misión. {@code target} filtra dentro del tipo; vacío = cualquiera. */
public enum MissionType {
    /** Matar criaturas vanilla. target: tipo de entidad (ZOMBIE). */
    KILL_MOB,
    /** Matar criaturas de RPGRoll-Mobs. target: id del mob. */
    KILL_RPG_MOB,
    /** Romper bloques que no puso un jugador. target: material (DIAMOND_ORE). */
    BREAK_BLOCK,
    /** Pescar algo. */
    FISH,
    /** Subir de nivel de personaje en RPGRoll. */
    LEVEL_UP,
    /** Completar una quest de RPGRoll-Quests. target: id de la quest. */
    COMPLETE_QUEST,
    /** Minutos jugados sin estar AFK. */
    PLAYTIME,
    /** Votar por el servidor. */
    VOTE,
    /** Reclamar la recompensa diaria. */
    CLAIM_DAILY
}
