package com.sack.rpgroll.guilds.guild.quest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Instancia activa (o completada) de una {@link GuildQuestDefinition} en una guild concreta. */
public class GuildQuestProgress {

    private final String questId;
    private final long startedAtMillis;
    private int currentAmount;
    private boolean completed;
    private long completedAtMillis;
    private final Map<UUID, Integer> contributions = new LinkedHashMap<>();

    public GuildQuestProgress(String questId) {
        this.questId = questId;
        this.startedAtMillis = System.currentTimeMillis();
    }

    public String questId() {
        return questId;
    }

    public long startedAtMillis() {
        return startedAtMillis;
    }

    public int currentAmount() {
        return currentAmount;
    }

    public boolean completed() {
        return completed;
    }

    public long completedAtMillis() {
        return completedAtMillis;
    }

    public Map<UUID, Integer> contributions() {
        return Map.copyOf(contributions);
    }

    /** @return true si esta contribución hizo que la quest se complete recién ahora. */
    public boolean addProgress(UUID contributorId, int amount, int targetAmount) {

        if (completed) {
            return false;
        }

        currentAmount += amount;
        contributions.merge(contributorId, amount, Integer::sum);

        if (currentAmount >= targetAmount) {
            completed = true;
            completedAtMillis = System.currentTimeMillis();
            return true;
        }

        return false;
    }

    public void restore(int currentAmount, boolean completed, long completedAtMillis,
            Map<UUID, Integer> contributions) {
        this.currentAmount = currentAmount;
        this.completed = completed;
        this.completedAtMillis = completedAtMillis;
        this.contributions.putAll(contributions);
    }

}
