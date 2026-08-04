package com.sack.rpgroll.mobs.engine;

import com.sack.rpgroll.mobs.core.MobSkill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Estado en memoria de un mob activo en el mundo: contribución de daño
 * por jugador (para loot PER_PLAYER), cooldowns de skill, fase actual,
 * progreso de patrulla y jugadores previamente detectados en rango.
 * No persiste — si el mob sobrevive un reinicio del server, este estado
 * se reconstruye vacío (aceptable: solo afecta loot-tracking/cooldowns
 * en curso, no la identidad del mob, que sí persiste vía PDC).
 */
public class ActiveMobState {

    private final UUID entityId;
    private final String definitionId;

    private int patrolIndex = 0;
    private int phaseIndex = -1;
    private long lastPeriodicMillis = 0;

    private final Map<UUID, Double> damageContribution = new HashMap<>();
    private final Map<String, Long> skillCooldowns = new HashMap<>();
    private final Set<UUID> playersInRange = new HashSet<>();
    private final List<MobSkill> activeSkills = new ArrayList<>();

    public ActiveMobState(UUID entityId, String definitionId, List<MobSkill> baseSkills) {
        this.entityId = entityId;
        this.definitionId = definitionId;
        this.activeSkills.addAll(baseSkills);
    }

    public UUID entityId() {
        return entityId;
    }

    public String definitionId() {
        return definitionId;
    }

    public int patrolIndex() {
        return patrolIndex;
    }

    public void advancePatrol(int max) {
        patrolIndex = max <= 0 ? 0 : (patrolIndex + 1) % max;
    }

    public int phaseIndex() {
        return phaseIndex;
    }

    public void setPhaseIndex(int phaseIndex) {
        this.phaseIndex = phaseIndex;
    }

    public boolean isPeriodicDue(long intervalMillis) {
        return System.currentTimeMillis() - lastPeriodicMillis >= intervalMillis;
    }

    public void markPeriodicFired() {
        lastPeriodicMillis = System.currentTimeMillis();
    }

    public void addDamageContribution(UUID player, double amount) {
        damageContribution.merge(player, amount, Double::sum);
    }

    public Map<UUID, Double> damageContribution() {
        return damageContribution;
    }

    public boolean isSkillOnCooldown(String skillId, long cooldownMillis) {
        Long last = skillCooldowns.get(skillId);
        return last != null && System.currentTimeMillis() - last < cooldownMillis;
    }

    public void markSkillUsed(String skillId) {
        skillCooldowns.put(skillId, System.currentTimeMillis());
    }

    public Set<UUID> playersInRange() {
        return playersInRange;
    }

    public List<MobSkill> activeSkills() {
        return activeSkills;
    }

    public void addSkills(List<MobSkill> skills) {
        activeSkills.addAll(skills);
    }

}
