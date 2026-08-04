package com.sack.rpgroll.dungeons.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Estado persistente por jugador — por ahora, solo cooldowns de dungeons completadas. */
public class DungeonPlayerState {

    private final UUID uuid;
    private final Map<String, Long> completedAt = new HashMap<>();

    public DungeonPlayerState(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public long getCompletedAt(String dungeonId) {
        return completedAt.getOrDefault(dungeonId.toLowerCase(), 0L);
    }

    public void markCompleted(String dungeonId, long nowMillis) {
        completedAt.put(dungeonId.toLowerCase(), nowMillis);
    }

    public Map<String, Long> allCompletions() {
        return completedAt;
    }

    public void restore(String dungeonId, long millis) {
        completedAt.put(dungeonId.toLowerCase(), millis);
    }

}
