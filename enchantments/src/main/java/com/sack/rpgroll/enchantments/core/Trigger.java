package com.sack.rpgroll.enchantments.core;

/**
 * Evento del juego que puede disparar la evaluación de un encantamiento.
 */
public enum Trigger {
    PLAYER_ATTACK,
    ENTITY_DAMAGE,
    BLOCK_BREAK,
    PLAYER_JUMP,
    PLAYER_MOVE,
    PLAYER_DEATH,
    ENTITY_KILL
}
