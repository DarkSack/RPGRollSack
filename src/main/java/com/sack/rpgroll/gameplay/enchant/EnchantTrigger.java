package com.sack.rpgroll.gameplay.enchant;

/**
 * Momento del gameplay en que se evalúa un encantamiento custom.
 * Extensible a futuro: ON_MINE_BLOCK, ON_DAMAGED, ON_BLOCK, etc.
 */
public enum EnchantTrigger {
    ON_HIT,
    ON_KILL,
    PASSIVE
}