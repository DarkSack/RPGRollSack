package com.sack.rpgroll.dungeons.core;

public enum LootScope {
    /** Se sortea una sola vez, entregado a quien completó el objetivo/dio el golpe final. */
    SHARED,
    /** Se sortea una vez por cada jugador del grupo. */
    PER_PLAYER,
    /** Se sortea una vez por jugador, ponderado por su contribución de daño a la corrida. */
    PER_CONTRIBUTION
}
