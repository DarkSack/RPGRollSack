package com.sack.rpgroll.mobs.core;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.common.reskin.EntityReskin;
import com.sack.rpgroll.mobs.util.DurationParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convierte un YAML de mobs/ (posiblemente en una subcarpeta de
 * categoría) en un {@link MobDefinition} completo. Solo "id" y
 * "base-entity-type" son obligatorios.
 */
public class MobParser implements ContentParser<MobDefinition> {

    @Override
    public MobDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String baseEntityType = config.getString("base-entity-type");
        if (baseEntityType == null || baseEntityType.isBlank()) {
            throw new IllegalArgumentException("mob '" + id + "' sin campo obligatorio 'base-entity-type'");
        }

        MobCategory category = parseEnum(config.getString("category"), MobCategory.class, MobCategory.NORMAL);
        String displayName = config.getString("display-name", id);
        String nameColor = config.getString("name-color");
        int level = config.getInt("level", 1);
        String rarityId = config.getString("rarity", "common");
        List<String> tags = config.getStringList("tags");
        String description = config.getString("description", "");

        MobModel model = parseModel(config.getConfigurationSection("model"), baseEntityType);
        Map<String, Double> stats = parseNumberMap(config.getConfigurationSection("stats"));
        Map<String, Double> resistances = parseNumberMap(config.getConfigurationSection("resistances"));
        List<MobWeakness> weaknesses = parseWeaknesses(config.getList("weaknesses"));
        AIBehaviorDef ai = parseAI(config.getConfigurationSection("ai"));
        List<MobSkill> skills = parseSkills(config.getList("skills"));
        Map<MobTrigger, List<MobAction>> triggers = parseTriggers(config.getConfigurationSection("triggers"));
        List<MobPhase> phases = parsePhases(config.getList("phases"));
        List<MobLootEntry> loot = parseLoot(config.getList("loot"));
        MobBossBarDef bossBar = parseBossBar(config.getConfigurationSection("bossbar"));
        List<MobDialogueLine> dialogues = parseDialogues(config.getList("dialogues"));
        SpawnRules spawnRules = parseSpawnRules(config.getConfigurationSection("spawn-rules"));
        Map<String, String> customData = parseStringMap(config.getConfigurationSection("custom-data"));

        return new MobDefinition(id, category, displayName, nameColor, level, rarityId, tags, description, model,
                stats, resistances, weaknesses, ai, skills, triggers, phases, loot, bossBar, dialogues, spawnRules,
                customData);
    }

    // ============ Helpers genéricos ============

    private <T extends Enum<T>> T parseEnum(String raw, Class<T> type, T fallback) {

        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private Map<String, Double> parseNumberMap(ConfigurationSection section) {

        Map<String, Double> result = new HashMap<>();

        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            if (section.get(key) instanceof Number number) {
                result.put(key.toLowerCase(Locale.ROOT), number.doubleValue());
            }
        }

        return result;
    }

    private Map<String, String> parseStringMap(ConfigurationSection section) {

        Map<String, String> result = new HashMap<>();

        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value != null) {
                result.put(key, value.toString());
            }
        }

        return result;
    }

    private int parseInt(Object raw, int fallback) {
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }

    private double parseDouble(Object raw, double fallback) {
        try {
            return Double.parseDouble(raw.toString().trim());
        } catch (NumberFormatException | NullPointerException e) {
            return fallback;
        }
    }

    // ============ Model ============

    private MobModel parseModel(ConfigurationSection section, String baseEntityType) {

        if (section == null) {
            return MobModel.defaults(baseEntityType.toUpperCase(Locale.ROOT));
        }

        double scale = section.getDouble("scale", 1.0);
        boolean glow = section.getBoolean("glow", false);
        boolean invisible = section.getBoolean("invisible", false);
        String modelEngineId = section.getString("model-engine-id");

        Map<String, String> equipment = new HashMap<>();
        ConfigurationSection equipmentSection = section.getConfigurationSection("equipment");

        if (equipmentSection != null) {
            for (String slot : equipmentSection.getKeys(false)) {
                equipment.put(slot.toUpperCase(Locale.ROOT), equipmentSection.getString(slot));
            }
        }

        return new MobModel(baseEntityType.toUpperCase(Locale.ROOT), scale, glow, invisible, equipment, modelEngineId,
                parseSkins(section));
    }

    private List<MobSkin> parseSkins(ConfigurationSection section) {

        List<Map<?, ?>> rawSkins = section.getMapList("skins");
        List<MobSkin> skins = new ArrayList<>();

        for (Map<?, ?> raw : rawSkins) {

            Object idValue = raw.get("id");
            if (idValue == null) {
                continue;
            }

            String material = raw.get("material") != null ? raw.get("material").toString() : null;
            int customModelData = raw.get("custom-model-data") != null
                    ? parseInt(raw.get("custom-model-data"), 0) : 0;
            double scale = raw.get("scale") != null ? parseDouble(raw.get("scale"), 1.0) : 1.0;
            double yOffset = raw.get("y-offset") != null ? parseDouble(raw.get("y-offset"), 0.0) : 0.0;
            double weight = raw.get("weight") != null ? parseDouble(raw.get("weight"), 1.0) : 1.0;

            skins.add(new MobSkin(idValue.toString(), new EntityReskin(material, customModelData, scale, yOffset),
                    weight));
        }

        return skins;
    }

    // ============ Weaknesses ============

    private List<MobWeakness> parseWeaknesses(List<?> raw) {

        List<MobWeakness> weaknesses = new ArrayList<>();

        if (raw == null) {
            return weaknesses;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("type") == null || map.get("key") == null) {
                continue;
            }

            try {
                MobWeaknessType type = MobWeaknessType.valueOf(map.get("type").toString().trim().toUpperCase(Locale.ROOT));
                String key = map.get("key").toString();
                double extra = map.get("extra-damage-percent") != null
                        ? parseDouble(map.get("extra-damage-percent"), 25)
                        : 25;

                weaknesses.add(new MobWeakness(type, key, extra));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return weaknesses;
    }

    // ============ AI ============

    private AIBehaviorDef parseAI(ConfigurationSection section) {

        if (section == null) {
            return new AIBehaviorDef(List.of("ATTACK_PLAYERS"), 16, 0, 1.0, List.of(), null);
        }

        List<String> goals = section.getStringList("goals");
        double aggroRange = section.getDouble("aggro-range", 16);
        double fleeHealthPercent = section.getDouble("flee-health-percent", 0);
        double moveSpeed = section.getDouble("move-speed", 1.0);
        List<String> patrolPoints = section.getStringList("patrol-points");
        String guardRegionId = section.getString("guard-region");

        return new AIBehaviorDef(goals, aggroRange, fleeHealthPercent, moveSpeed, patrolPoints, guardRegionId);
    }

    // ============ Skills / Triggers / Actions ============

    private List<MobAction> parseActions(List<?> raw) {

        List<MobAction> actions = new ArrayList<>();

        if (raw == null) {
            return actions;
        }

        for (Object entry : raw) {

            if (entry instanceof String simple) {
                actions.add(new MobAction(simple.trim().toUpperCase(Locale.ROOT), Map.of()));
                continue;
            }

            if (!(entry instanceof Map<?, ?> map) || map.get("type") == null) {
                continue;
            }

            Map<String, String> params = new HashMap<>();

            for (var mapEntry : map.entrySet()) {

                String key = mapEntry.getKey().toString();

                if (key.equals("type") || mapEntry.getValue() == null) {
                    continue;
                }

                params.put(key, mapEntry.getValue().toString());
            }

            actions.add(new MobAction(map.get("type").toString().trim().toUpperCase(Locale.ROOT), params));
        }

        return actions;
    }

    private List<MobSkill> parseSkills(List<?> raw) {

        List<MobSkill> skills = new ArrayList<>();

        if (raw == null) {
            return skills;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("id") == null) {
                continue;
            }

            String id = map.get("id").toString();
            String displayName = map.get("display-name") != null ? map.get("display-name").toString() : id;

            MobTrigger trigger = null;
            if (map.get("trigger") != null) {
                try {
                    trigger = MobTrigger.valueOf(map.get("trigger").toString().trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }

            long cooldownMillis = DurationParser.parseMillis(
                    map.get("cooldown") != null ? map.get("cooldown").toString() : "");
            double chance = map.get("chance") != null ? parseDouble(map.get("chance"), 100) : 100;

            List<String> conditions = map.get("conditions") instanceof List<?> conditionList
                    ? conditionList.stream().map(Object::toString).toList()
                    : List.of();

            List<MobAction> actions = map.get("actions") instanceof List<?> actionList
                    ? parseActions(actionList)
                    : List.of();

            skills.add(new MobSkill(id, displayName, trigger, cooldownMillis, chance, conditions, actions));
        }

        return skills;
    }

    private Map<MobTrigger, List<MobAction>> parseTriggers(ConfigurationSection section) {

        Map<MobTrigger, List<MobAction>> triggers = new EnumMap<>(MobTrigger.class);

        if (section == null) {
            return triggers;
        }

        for (String key : section.getKeys(false)) {

            try {
                MobTrigger trigger = MobTrigger.valueOf(key.trim().toUpperCase(Locale.ROOT));
                triggers.put(trigger, parseActions(section.getList(key)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return triggers;
    }

    // ============ Phases ============

    private List<MobPhase> parsePhases(List<?> raw) {

        List<MobPhase> phases = new ArrayList<>();

        if (raw == null) {
            return phases;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("id") == null) {
                continue;
            }

            String id = map.get("id").toString();
            double threshold = map.get("health-threshold-percent") != null
                    ? parseDouble(map.get("health-threshold-percent"), 100)
                    : 100;

            Map<String, Double> statMultipliers = new HashMap<>();
            if (map.get("stat-multipliers") instanceof Map<?, ?> multipliersMap) {
                for (var multEntry : multipliersMap.entrySet()) {
                    if (multEntry.getValue() instanceof Number number) {
                        statMultipliers.put(multEntry.getKey().toString().toLowerCase(Locale.ROOT),
                                number.doubleValue());
                    }
                }
            }

            List<MobSkill> phaseSkills = map.get("skills") instanceof List<?> skillList
                    ? parseSkills(skillList)
                    : List.of();

            String bossBarColor = map.get("bossbar-color") != null ? map.get("bossbar-color").toString() : null;
            String bossBarTitle = map.get("bossbar-title") != null ? map.get("bossbar-title").toString() : null;
            String dialogueLine = map.get("dialogue") != null ? map.get("dialogue").toString() : null;
            String particleEffect = map.get("particle") != null ? map.get("particle").toString() : null;

            phases.add(new MobPhase(id, threshold, statMultipliers, phaseSkills, bossBarColor, bossBarTitle,
                    dialogueLine, particleEffect));
        }

        return phases;
    }

    // ============ Loot ============

    private List<MobLootEntry> parseLoot(List<?> raw) {

        List<MobLootEntry> loot = new ArrayList<>();

        if (raw == null) {
            return loot;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("type") == null) {
                continue;
            }

            LootType type;
            try {
                type = LootType.valueOf(map.get("type").toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                continue;
            }

            String reference = map.get("reference") != null ? map.get("reference").toString() : null;
            int amountMin = map.get("amount-min") != null ? parseInt(map.get("amount-min"), 1) : 1;
            int amountMax = map.get("amount-max") != null ? parseInt(map.get("amount-max"), amountMin) : amountMin;
            double chance = map.get("chance") != null ? parseDouble(map.get("chance"), 100) : 100;
            int requiredLevel = map.get("required-level") != null ? parseInt(map.get("required-level"), 0) : 0;

            LootScope scope = LootScope.SHARED;
            if (map.get("scope") != null) {
                try {
                    scope = LootScope.valueOf(map.get("scope").toString().trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }

            loot.add(new MobLootEntry(type, reference, amountMin, amountMax, chance, requiredLevel, scope));
        }

        return loot;
    }

    // ============ BossBar / Dialogues / Spawn Rules ============

    private MobBossBarDef parseBossBar(ConfigurationSection section) {

        if (section == null) {
            return MobBossBarDef.disabled();
        }

        return new MobBossBarDef(
                section.getBoolean("enabled", true),
                section.getString("color", "RED"),
                section.getString("style", "SOLID"),
                section.getString("title"));
    }

    private List<MobDialogueLine> parseDialogues(List<?> raw) {

        List<MobDialogueLine> dialogues = new ArrayList<>();

        if (raw == null) {
            return dialogues;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("text") == null) {
                continue;
            }

            MobTrigger trigger = null;
            if (map.get("trigger") != null) {
                try {
                    trigger = MobTrigger.valueOf(map.get("trigger").toString().trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }

            String phaseId = map.get("phase") != null ? map.get("phase").toString() : null;

            dialogues.add(new MobDialogueLine(trigger, phaseId, map.get("text").toString()));
        }

        return dialogues;
    }

    private SpawnRules parseSpawnRules(ConfigurationSection section) {

        if (section == null) {
            return SpawnRules.none();
        }

        List<String> biomes = section.getStringList("biomes");
        List<String> regions = section.getStringList("regions");
        List<String> worlds = section.getStringList("worlds");

        int hourMin = -1;
        int hourMax = -1;
        String hourRange = section.getString("hour");

        if (hourRange != null && hourRange.contains("-")) {
            String[] parts = hourRange.split("-", 2);
            hourMin = parseInt(parts[0], -1);
            hourMax = parseInt(parts[1], -1);
        }

        String weather = section.getString("weather");
        int minHeight = section.getInt("min-height", -64);
        int maxHeight = section.getInt("max-height", 320);
        double minDistance = section.getDouble("min-distance-from-players", 0);
        boolean naturalSpawn = section.getBoolean("natural-spawn", false);
        double spawnWeight = section.getDouble("spawn-weight", 1.0);

        return new SpawnRules(biomes, regions, worlds, hourMin, hourMax, weather, minHeight, maxHeight, minDistance,
                naturalSpawn, spawnWeight);
    }

}
