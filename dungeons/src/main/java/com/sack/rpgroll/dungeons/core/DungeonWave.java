package com.sack.rpgroll.dungeons.core;

import java.util.List;

/**
 * Una oleada de enemigos dentro de una sala de combate. Las oleadas de
 * una sala corren en orden — la siguiente arranca recién cuando la
 * anterior está completamente derrotada (o vence su {@code timeLimitMillis},
 * si tiene uno, lo que cuenta como fallo de la sala).
 */
public record DungeonWave(String id, List<DungeonWaveMob> mobs, long timeLimitMillis, long delayBeforeMillis) {

    public DungeonWave {
        mobs = mobs == null ? List.of() : List.copyOf(mobs);
    }

    public int totalMobCount() {
        return mobs.stream().mapToInt(DungeonWaveMob::amount).sum();
    }

}
