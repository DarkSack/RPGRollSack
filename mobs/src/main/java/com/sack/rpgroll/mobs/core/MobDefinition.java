package com.sack.rpgroll.mobs.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Define un mob RPGRoll completo. Cada componente descrito en la
 * filosofía del addon (Identity, Model, Attributes, Equipment —dentro de
 * Model—, Skills, Phases, Triggers, Conditions —dentro de Skills—, Drops
 * —dentro de Loot—, Dialogues, BossBar, Resistances, Weaknesses, Spawn
 * Rules, Custom Data) es un campo independiente.
 */
public record MobDefinition(
        String id,
        MobCategory category,
        String displayName,
        String nameColor,
        int level,
        String rarityId,
        List<String> tags,
        String description,
        MobModel model,
        Map<String, Double> stats,
        Map<String, Double> resistances,
        List<MobWeakness> weaknesses,
        AIBehaviorDef ai,
        List<MobSkill> skills,
        Map<MobTrigger, List<MobAction>> triggers,
        List<MobPhase> phases,
        List<MobLootEntry> loot,
        MobBossBarDef bossBar,
        List<MobDialogueLine> dialogues,
        SpawnRules spawnRules,
        Map<String, String> customData) implements RPGContent {

    public MobDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(model, "model no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        category = category == null ? MobCategory.NORMAL : category;
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        rarityId = rarityId == null || rarityId.isBlank() ? "common" : rarityId;
        tags = tags == null ? List.of() : List.copyOf(tags);
        description = description == null ? "" : description;
        level = Math.max(1, level);
        stats = stats == null ? Map.of() : Map.copyOf(stats);
        resistances = resistances == null ? Map.of() : Map.copyOf(resistances);
        weaknesses = weaknesses == null ? List.of() : List.copyOf(weaknesses);
        ai = ai == null ? new AIBehaviorDef(List.of("ATTACK_PLAYERS"), 16, 0, 1.0, List.of(), null) : ai;
        skills = skills == null ? List.of() : List.copyOf(skills);
        triggers = triggers == null ? Map.of() : Map.copyOf(triggers);
        phases = phases == null ? List.of() : List.copyOf(phases);
        loot = loot == null ? List.of() : List.copyOf(loot);
        bossBar = bossBar == null ? MobBossBarDef.disabled() : bossBar;
        dialogues = dialogues == null ? List.of() : List.copyOf(dialogues);
        spawnRules = spawnRules == null ? SpawnRules.none() : spawnRules;
        customData = customData == null ? Map.of() : Map.copyOf(customData);
    }

    public List<MobAction> actionsFor(MobTrigger trigger) {
        return triggers.getOrDefault(trigger, List.of());
    }

    public double stat(String key) {
        return stats.getOrDefault(key, 0.0);
    }

    /**
     * La fase más avanzada ya alcanzada a este % de vida — entre todas las
     * fases cuyo umbral ya fue cruzado (healthPercent &lt;= umbral), la de
     * umbral más bajo (la más profunda en el combate).
     */
    public Optional<MobPhase> phaseFor(double healthPercent) {
        return phases.stream()
                .filter(phase -> healthPercent <= phase.healthThresholdPercent())
                .min(Comparator.comparingDouble(MobPhase::healthThresholdPercent));
    }

    public boolean isBoss() {
        return category == MobCategory.MINI_BOSS || category == MobCategory.WORLD_BOSS
                || category == MobCategory.RAID_BOSS || category == MobCategory.DUNGEON_BOSS
                || category == MobCategory.CINEMATIC_BOSS;
    }

}
