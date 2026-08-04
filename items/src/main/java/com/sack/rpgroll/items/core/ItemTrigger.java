package com.sack.rpgroll.items.core;

/**
 * Evento del juego que puede disparar el "comportamiento" de un ítem
 * (triggers -> conditions -> actions). Ver
 * {@link com.sack.rpgroll.items.listener.ItemTriggerListener}.
 */
public enum ItemTrigger {
    EQUIP,
    UNEQUIP,
    RIGHT_CLICK,
    LEFT_CLICK,
    ENTITY_HIT,
    ENTITY_KILL,
    BLOCK_BREAK,
    BLOCK_PLACE,
    PLAYER_DAMAGE,
    PLAYER_DEATH,
    PLAYER_RESPAWN,
    PLAYER_MOVE,
    PLAYER_JUMP,
    PLAYER_SNEAK,
    PLAYER_SPRINT,
    PLAYER_INTERACT,
    CONSUME,
    THROW,
    PICKUP,
    DROP
}
