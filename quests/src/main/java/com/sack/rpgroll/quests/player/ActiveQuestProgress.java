package com.sack.rpgroll.quests.player;

import java.util.HashMap;
import java.util.Map;

/**
 * Progreso mutable de una quest activa: en qué stage está y cuánto avanzó
 * cada objetivo de ESE stage (los contadores se reinician al cambiar de
 * stage — cada stage empieza su propio conteo).
 */
public class ActiveQuestProgress {

    private final String questId;
    private final long startedAtMillis;
    private int stageIndex;
    private long stageStartedAtMillis;
    private final Map<Integer, Integer> objectiveProgress = new HashMap<>();

    public ActiveQuestProgress(String questId, int stageIndex, long startedAtMillis) {
        this.questId = questId;
        this.stageIndex = stageIndex;
        this.startedAtMillis = startedAtMillis;
        this.stageStartedAtMillis = startedAtMillis;
    }

    public String questId() {
        return questId;
    }

    public long startedAtMillis() {
        return startedAtMillis;
    }

    public long stageStartedAtMillis() {
        return stageStartedAtMillis;
    }

    public int stageIndex() {
        return stageIndex;
    }

    public void setStageIndex(int stageIndex) {
        this.stageIndex = stageIndex;
        this.stageStartedAtMillis = System.currentTimeMillis();
        this.objectiveProgress.clear();
    }

    public int getProgress(int objectiveIndex) {
        return objectiveProgress.getOrDefault(objectiveIndex, 0);
    }

    public void addProgress(int objectiveIndex, int amount) {
        objectiveProgress.merge(objectiveIndex, amount, Integer::sum);
    }

    public Map<Integer, Integer> allProgress() {
        return objectiveProgress;
    }

    public void restoreProgress(Map<Integer, Integer> progress) {
        objectiveProgress.clear();
        objectiveProgress.putAll(progress);
    }

    public void restoreStageStartedAt(long millis) {
        this.stageStartedAtMillis = millis;
    }

}
