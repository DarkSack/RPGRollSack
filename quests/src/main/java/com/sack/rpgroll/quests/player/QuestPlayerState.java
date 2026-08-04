package com.sack.rpgroll.quests.player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Estado de progresión de quests de un jugador: activas, completadas (con
 * timestamp, para cooldown de repetibles), fallidas, y estadísticas
 * agregadas. Mutable — vive en memoria mientras el jugador está online y
 * se persiste vía {@link QuestPlayerStateStore}.
 */
public class QuestPlayerState {

    private final UUID uuid;
    private final Map<String, ActiveQuestProgress> active = new HashMap<>();
    private final Map<String, Long> completed = new HashMap<>();
    private final Set<String> failed = new HashSet<>();
    private long totalTimeSpentMillis;
    private int totalCompletedCount;

    public QuestPlayerState(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public boolean isActive(String questId) {
        return active.containsKey(questId);
    }

    public Optional<ActiveQuestProgress> getActive(String questId) {
        return Optional.ofNullable(active.get(questId));
    }

    public Map<String, ActiveQuestProgress> allActive() {
        return active;
    }

    public ActiveQuestProgress startQuest(String questId, long nowMillis) {
        ActiveQuestProgress progress = new ActiveQuestProgress(questId, 0, nowMillis);
        active.put(questId, progress);
        return progress;
    }

    public void abandonQuest(String questId) {
        active.remove(questId);
    }

    public void completeQuest(String questId, long nowMillis) {

        ActiveQuestProgress progress = active.remove(questId);

        if (progress != null) {
            totalTimeSpentMillis += Math.max(0, nowMillis - progress.startedAtMillis());
        }

        completed.put(questId, nowMillis);
        totalCompletedCount++;
    }

    public void failQuest(String questId) {
        active.remove(questId);
        failed.add(questId);
    }

    /** Admin: borra todo rastro de una quest (activa/completada/fallida) para poder reiniciarla. */
    public void resetQuest(String questId) {
        active.remove(questId);
        completed.remove(questId);
        failed.remove(questId);
    }

    public boolean hasCompleted(String questId) {
        return completed.containsKey(questId);
    }

    public boolean hasFailed(String questId) {
        return failed.contains(questId);
    }

    public Optional<Long> completedAt(String questId) {
        return Optional.ofNullable(completed.get(questId));
    }

    public Map<String, Long> allCompleted() {
        return completed;
    }

    public Set<String> allFailed() {
        return failed;
    }

    public long totalTimeSpentMillis() {
        return totalTimeSpentMillis;
    }

    public void addTimeSpent(long millis) {
        totalTimeSpentMillis += millis;
    }

    public int totalCompletedCount() {
        return totalCompletedCount;
    }

    public void setTotalCompletedCount(int totalCompletedCount) {
        this.totalCompletedCount = totalCompletedCount;
    }

}
