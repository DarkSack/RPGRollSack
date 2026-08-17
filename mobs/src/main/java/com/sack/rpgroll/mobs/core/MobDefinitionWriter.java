package com.sack.rpgroll.mobs.core;

import com.sack.rpgroll.common.reskin.EntityReskin;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Serializa un {@link MobDefinition} completo de vuelta a YAML — inverso de {@link MobParser}. */
public class MobDefinitionWriter {

    public void save(MobDefinition definition, File file) throws IOException {

        YamlConfiguration config = new YamlConfiguration();

        config.set("id", definition.id());
        config.set("category", definition.category().name());
        config.set("display-name", definition.displayName());

        if (definition.nameColor() != null) {
            config.set("name-color", definition.nameColor());
        }

        config.set("level", definition.level());
        config.set("rarity", definition.rarityId());
        config.set("tags", definition.tags());
        config.set("description", definition.description());
        config.set("base-entity-type", definition.model().baseEntityType());

        writeModel(config, definition.model());
        writeMap(config, "stats", definition.stats());
        writeMap(config, "resistances", definition.resistances());
        writeWeaknesses(config, definition.weaknesses());
        writeAI(config, definition.ai());
        writeSkills(config, "skills", definition.skills());
        writeTriggers(config, definition.triggers());
        writePhases(config, definition.phases());
        writeLoot(config, definition.loot());
        writeBossBar(config, definition.bossBar());
        writeDialogues(config, definition.dialogues());
        writeSpawnRules(config, definition.spawnRules());
        writeMap(config, "custom-data", definition.customData());

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        config.save(file);
    }

    private void writeMap(YamlConfiguration config, String path, Map<String, ?> map) {
        for (var entry : map.entrySet()) {
            config.set(path + "." + entry.getKey(), entry.getValue());
        }
    }

    private void writeModel(YamlConfiguration config, MobModel model) {

        config.set("model.scale", model.scale());
        config.set("model.glow", model.glow());
        config.set("model.invisible", model.invisible());

        if (model.modelEngineId() != null) {
            config.set("model.model-engine-id", model.modelEngineId());
        }

        for (var entry : model.equipment().entrySet()) {
            config.set("model.equipment." + entry.getKey(), entry.getValue());
        }

        writeSkins(config, model.skins());
    }

    private void writeSkins(YamlConfiguration config, List<MobSkin> skins) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (MobSkin skin : skins) {

            EntityReskin reskin = skin.reskin();
            Map<String, Object> map = new HashMap<>();
            map.put("id", skin.id());

            if (reskin.material() != null) {
                map.put("material", reskin.material());
            }

            map.put("custom-model-data", reskin.customModelData());
            map.put("scale", reskin.scale());
            map.put("y-offset", reskin.yOffset());
            map.put("weight", skin.weight());

            raw.add(map);
        }

        if (!raw.isEmpty()) {
            config.set("model.skins", raw);
        }
    }

    private void writeWeaknesses(YamlConfiguration config, List<MobWeakness> weaknesses) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (MobWeakness weakness : weaknesses) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("type", weakness.type().name());
            entry.put("key", weakness.key());
            entry.put("extra-damage-percent", weakness.extraDamagePercent());
            raw.add(entry);
        }

        if (!raw.isEmpty()) {
            config.set("weaknesses", raw);
        }
    }

    private void writeAI(YamlConfiguration config, AIBehaviorDef ai) {
        config.set("ai.goals", ai.goals());
        config.set("ai.aggro-range", ai.aggroRange());
        config.set("ai.flee-health-percent", ai.fleeHealthPercent());
        config.set("ai.move-speed", ai.moveSpeed());
        config.set("ai.patrol-points", ai.patrolPoints());

        if (ai.guardRegionId() != null) {
            config.set("ai.guard-region", ai.guardRegionId());
        }
    }

    private void writeSkills(YamlConfiguration config, String path, List<MobSkill> skills) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (MobSkill skill : skills) {

            Map<String, Object> entry = new HashMap<>();
            entry.put("id", skill.id());
            entry.put("display-name", skill.displayName());

            if (skill.trigger() != null) {
                entry.put("trigger", skill.trigger().name());
            }

            entry.put("cooldown", (skill.cooldownMillis() / 1000) + "s");
            entry.put("chance", skill.chance());
            entry.put("conditions", skill.conditions());
            entry.put("actions", actionsToRaw(skill.actions()));

            raw.add(entry);
        }

        if (!raw.isEmpty()) {
            config.set(path, raw);
        }
    }

    private void writeTriggers(YamlConfiguration config, Map<MobTrigger, List<MobAction>> triggers) {

        for (var entry : triggers.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                config.set("triggers." + entry.getKey().name(), actionsToRaw(entry.getValue()));
            }
        }
    }

    private void writePhases(YamlConfiguration config, List<MobPhase> phases) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (MobPhase phase : phases) {

            Map<String, Object> entry = new HashMap<>();
            entry.put("id", phase.id());
            entry.put("health-threshold-percent", phase.healthThresholdPercent());
            entry.put("stat-multipliers", phase.statMultipliers());
            entry.put("skills", skillsToRaw(phase.skills()));

            if (phase.bossBarColor() != null) {
                entry.put("bossbar-color", phase.bossBarColor());
            }
            if (phase.bossBarTitle() != null) {
                entry.put("bossbar-title", phase.bossBarTitle());
            }
            if (phase.dialogueLine() != null) {
                entry.put("dialogue", phase.dialogueLine());
            }
            if (phase.particleEffect() != null) {
                entry.put("particle", phase.particleEffect());
            }

            raw.add(entry);
        }

        if (!raw.isEmpty()) {
            config.set("phases", raw);
        }
    }

    private void writeLoot(YamlConfiguration config, List<MobLootEntry> loot) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (MobLootEntry entry : loot) {

            Map<String, Object> map = new HashMap<>();
            map.put("type", entry.type().name());

            if (entry.reference() != null) {
                map.put("reference", entry.reference());
            }

            map.put("amount-min", entry.amountMin());
            map.put("amount-max", entry.amountMax());
            map.put("chance", entry.chance());
            map.put("required-level", entry.requiredLevel());
            map.put("scope", entry.scope().name());

            raw.add(map);
        }

        if (!raw.isEmpty()) {
            config.set("loot", raw);
        }
    }

    private void writeBossBar(YamlConfiguration config, MobBossBarDef bossBar) {

        config.set("bossbar.enabled", bossBar.enabled());
        config.set("bossbar.color", bossBar.color());
        config.set("bossbar.style", bossBar.style());

        if (bossBar.title() != null) {
            config.set("bossbar.title", bossBar.title());
        }
    }

    private void writeDialogues(YamlConfiguration config, List<MobDialogueLine> dialogues) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (MobDialogueLine line : dialogues) {

            Map<String, Object> entry = new HashMap<>();
            entry.put("text", line.text());

            if (line.trigger() != null) {
                entry.put("trigger", line.trigger().name());
            }
            if (line.phaseId() != null) {
                entry.put("phase", line.phaseId());
            }

            raw.add(entry);
        }

        if (!raw.isEmpty()) {
            config.set("dialogues", raw);
        }
    }

    private void writeSpawnRules(YamlConfiguration config, SpawnRules rules) {

        config.set("spawn-rules.biomes", rules.biomes());
        config.set("spawn-rules.regions", rules.regions());
        config.set("spawn-rules.worlds", rules.worlds());

        if (rules.hasTimeRange()) {
            config.set("spawn-rules.hour", rules.hourMin() + "-" + rules.hourMax());
        }
        if (rules.weather() != null) {
            config.set("spawn-rules.weather", rules.weather());
        }

        config.set("spawn-rules.min-height", rules.minHeight());
        config.set("spawn-rules.max-height", rules.maxHeight());
        config.set("spawn-rules.min-distance-from-players", rules.minDistanceFromPlayers());
        config.set("spawn-rules.natural-spawn", rules.naturalSpawn());
        config.set("spawn-rules.spawn-weight", rules.spawnWeight());
    }

    private List<Map<String, Object>> skillsToRaw(List<MobSkill> skills) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (MobSkill skill : skills) {

            Map<String, Object> entry = new HashMap<>();
            entry.put("id", skill.id());
            entry.put("display-name", skill.displayName());

            if (skill.trigger() != null) {
                entry.put("trigger", skill.trigger().name());
            }

            entry.put("cooldown", (skill.cooldownMillis() / 1000) + "s");
            entry.put("chance", skill.chance());
            entry.put("conditions", skill.conditions());
            entry.put("actions", actionsToRaw(skill.actions()));

            raw.add(entry);
        }

        return raw;
    }

    private List<Map<String, Object>> actionsToRaw(List<MobAction> actions) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (MobAction action : actions) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("type", action.type());
            entry.putAll(action.params());
            raw.add(entry);
        }

        return raw;
    }

}
