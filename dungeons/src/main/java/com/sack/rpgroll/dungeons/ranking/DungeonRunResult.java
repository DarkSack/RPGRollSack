package com.sack.rpgroll.dungeons.ranking;

import java.util.List;

/** Una corrida completada, tal como queda registrada en el ranking. */
public record DungeonRunResult(
        String dungeonId,
        String difficultyId,
        List<String> playerNames,
        long completedAtMillis,
        long durationMillis,
        int deaths,
        double totalDamageDealt) {

    /** Cuanto más alto, mejor — penaliza duración y muertes. Nunca negativo. */
    public double score() {
        double raw = 10_000 - (durationMillis / 1000.0) - (deaths * 30.0) + (totalDamageDealt * 0.1);
        return Math.max(0, raw);
    }

}
