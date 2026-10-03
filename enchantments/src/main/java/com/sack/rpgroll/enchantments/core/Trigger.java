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
    ENTITY_KILL,
    /** Clic derecho sobre un bloque con la mano principal (labrar, por ejemplo). */
    BLOCK_INTERACT,
    /** El jugador para un golpe con el escudo. */
    SHIELD_BLOCK,
    /** Un hacha le desactiva el escudo al jugador. */
    SHIELD_DISABLE,
    /** El jugador recoge experiencia (lo que sobra tras el Reparación vanilla). */
    EXP_PICKUP
}
