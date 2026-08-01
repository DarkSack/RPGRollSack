package com.sack.rpgroll.crates.core;

/**
 * Una acción a ejecutar cuando una CrateReward resulta sorteada.
 */
public record CrateAction(CrateActionType type, String value) {

    public enum CrateActionType {
        MESSAGE,
        COMMAND,
        GIVE_ITEM,
        SOUND
    }

}
