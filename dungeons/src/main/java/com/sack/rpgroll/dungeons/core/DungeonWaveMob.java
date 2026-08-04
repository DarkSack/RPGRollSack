package com.sack.rpgroll.dungeons.core;

/** Una entrada de mob dentro de una oleada — {@code mobId} referencia una definición de RPGRoll-Mobs. */
public record DungeonWaveMob(String mobId, int amount) {

    public DungeonWaveMob {
        amount = Math.max(1, amount);
    }

}
