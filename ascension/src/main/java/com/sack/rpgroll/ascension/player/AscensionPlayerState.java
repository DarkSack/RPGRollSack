package com.sack.rpgroll.ascension.player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Estado de progresión avanzada de un jugador: evolución/especialización
 * activas, talentos desbloqueados, prestigio, legado, afinidades,
 * dominio, reputación, logros y títulos. Mutable — vive en memoria
 * mientras está online, persistido vía {@link AscensionPlayerStateStore}.
 */
public class AscensionPlayerState {

    private final UUID uuid;

    private String currentEvolutionId;
    private String currentSpecializationId;
    private final Set<String> unlockedTalents = new HashSet<>();
    private int availableTalentPoints;

    private int prestigeCount;
    private int legacyCount;
    private double permanentExpBonusPercent;

    private final Map<String, Integer> affinityExperience = new HashMap<>();
    private final Map<String, Integer> masteryExperience = new HashMap<>();
    private final Map<String, Integer> reputation = new HashMap<>();

    private final Set<String> unlockedAchievements = new HashSet<>();
    private final Set<String> unlockedTitles = new HashSet<>();
    private String activeTitle;

    public AscensionPlayerState(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public String getCurrentEvolutionId() {
        return currentEvolutionId;
    }

    public void setCurrentEvolutionId(String currentEvolutionId) {
        this.currentEvolutionId = currentEvolutionId;
    }

    public String getCurrentSpecializationId() {
        return currentSpecializationId;
    }

    public void setCurrentSpecializationId(String currentSpecializationId) {
        this.currentSpecializationId = currentSpecializationId;
    }

    public Set<String> getUnlockedTalents() {
        return unlockedTalents;
    }

    public boolean hasUnlockedTalent(String talentId) {
        return unlockedTalents.contains(talentId);
    }

    public int getAvailableTalentPoints() {
        return availableTalentPoints;
    }

    public void addTalentPoints(int amount) {
        availableTalentPoints += amount;
    }

    public void spendTalentPoints(int amount) {
        availableTalentPoints = Math.max(0, availableTalentPoints - amount);
    }

    public int getPrestigeCount() {
        return prestigeCount;
    }

    public void incrementPrestige() {
        prestigeCount++;
    }

    public int getLegacyCount() {
        return legacyCount;
    }

    public void incrementLegacy() {
        legacyCount++;
    }

    public double getPermanentExpBonusPercent() {
        return permanentExpBonusPercent;
    }

    public void addPermanentExpBonusPercent(double amount) {
        permanentExpBonusPercent += amount;
    }

    public Map<String, Integer> getAffinityExperience() {
        return affinityExperience;
    }

    public int getAffinityExperience(String affinityId) {
        return affinityExperience.getOrDefault(affinityId, 0);
    }

    public void addAffinityExperience(String affinityId, int amount) {
        affinityExperience.merge(affinityId, amount, Integer::sum);
    }

    public Map<String, Integer> getMasteryExperience() {
        return masteryExperience;
    }

    public int getMasteryExperience(String category) {
        return masteryExperience.getOrDefault(category, 0);
    }

    public void addMasteryExperience(String category, int amount) {
        masteryExperience.merge(category, amount, Integer::sum);
    }

    public Map<String, Integer> getReputation() {
        return reputation;
    }

    public int getReputation(String factionId) {
        return reputation.getOrDefault(factionId, 0);
    }

    public void addReputation(String factionId, int amount) {
        reputation.merge(factionId, amount, Integer::sum);
    }

    public Set<String> getUnlockedAchievements() {
        return unlockedAchievements;
    }

    public boolean unlockAchievement(String id) {
        return unlockedAchievements.add(id);
    }

    public Set<String> getUnlockedTitles() {
        return unlockedTitles;
    }

    public boolean unlockTitle(String id) {
        return unlockedTitles.add(id);
    }

    public String getActiveTitle() {
        return activeTitle;
    }

    public void setActiveTitle(String activeTitle) {
        this.activeTitle = activeTitle;
    }

    /**
     * Reinicio total de personaje (raza evolucionada, especialización,
     * talentos y prestigio) — solo lo usa el sistema de Legado. El
     * prestigio normal NO llama esto: solo resetea el nivel del jugador y
     * suma su bono, conservando evolución/especialización/talentos.
     */
    public void resetForLegacy() {
        currentEvolutionId = null;
        currentSpecializationId = null;
        unlockedTalents.clear();
        availableTalentPoints = 0;
        prestigeCount = 0;
    }

}
