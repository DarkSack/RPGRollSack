package com.sack.rpgroll.dungeons.core;

/**
 * Cada sala alcanzada es implícitamente un checkpoint — no hay una lista
 * separada de puntos de guardado, la política define qué pasa al morir
 * el grupo (o quedarse sin reintentos) respecto de la última sala
 * alcanzada.
 */
public record DungeonCheckpointPolicy(CheckpointMode mode, boolean sharedLives, int maxRetries) {

    public DungeonCheckpointPolicy {
        mode = mode == null ? CheckpointMode.REVIVE_AT_CHECKPOINT : mode;
        maxRetries = maxRetries < 0 ? -1 : maxRetries;
    }

    public static DungeonCheckpointPolicy defaultPolicy() {
        return new DungeonCheckpointPolicy(CheckpointMode.REVIVE_AT_CHECKPOINT, false, -1);
    }

    /** @return true si {@code maxRetries} es ilimitado (negativo). */
    public boolean unlimitedRetries() {
        return maxRetries < 0;
    }

}
