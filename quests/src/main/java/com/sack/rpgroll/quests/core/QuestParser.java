package com.sack.rpgroll.quests.core;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.quests.util.DurationParser;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convierte un YAML de quests/ en un {@link Quest} completo: requisitos,
 * stages (objetivos, condiciones, diálogo, eventos), recompensas y eventos
 * de la quest. Solo "id" y al menos un stage con al menos un objetivo son
 * obligatorios — todo lo demás cae a un default seguro si falta.
 */
public class QuestParser implements ContentParser<Quest> {

    @Override
    public Quest parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        QuestCategory category = parseEnum(config.getString("category"), QuestCategory.class, QuestCategory.SIDE_QUEST);
        QuestDifficulty difficulty = parseEnum(config.getString("difficulty"), QuestDifficulty.class,
                QuestDifficulty.NORMAL);

        boolean repeatable = config.getBoolean("repeatable", false);
        long cooldownMillis = DurationParser.parseMillis(config.getString("cooldown", ""));

        QuestRequirements requirements = parseRequirements(config.getConfigurationSection("requirements"));
        List<QuestStage> stages = parseStages(config.getList("stages"), id);
        QuestRewards rewards = parseRewards(config.getConfigurationSection("rewards"));
        Map<QuestEventType, List<QuestAction>> events = parseEvents(config.getConfigurationSection("events"));

        return new Quest(id, displayName, category, difficulty, repeatable, cooldownMillis,
                requirements, stages, rewards, events);
    }

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

    // ============ Requirements ============

    private QuestRequirements parseRequirements(ConfigurationSection section) {

        if (section == null) {
            return QuestRequirements.none();
        }

        int level = section.getInt("level", 0);
        String race = section.getString("race");
        String playerClass = section.getString("class");
        String profession = section.getString("profession");
        String trait = section.getString("trait");
        String permission = section.getString("permission");
        double money = section.getDouble("money", 0);

        List<ItemRequirement> items = parseItemRequirements(section.getMapList("items"));
        List<String> completed = section.getStringList("completed");

        String world = section.getString("world");
        String region = section.getString("region");
        String biome = section.getString("biome");
        String weather = section.getString("weather");

        int hourMin = -1;
        int hourMax = -1;

        String hourRange = section.getString("hour");
        if (hourRange != null && hourRange.contains("-")) {
            String[] parts = hourRange.split("-", 2);
            try {
                hourMin = Integer.parseInt(parts[0].trim());
                hourMax = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
            }
        }

        return new QuestRequirements(level, race, playerClass, profession, trait, permission, money,
                items, completed, world, region, biome, weather, hourMin, hourMax);
    }

    private List<ItemRequirement> parseItemRequirements(List<Map<?, ?>> raw) {

        List<ItemRequirement> items = new ArrayList<>();

        for (Map<?, ?> entry : raw) {

            Object materialObj = entry.get("material");
            if (materialObj == null) {
                continue;
            }

            Material material = parseMaterial(materialObj.toString());
            if (material == null) {
                continue;
            }

            int amount = entry.get("amount") != null ? parseInt(entry.get("amount").toString(), 1) : 1;
            items.add(new ItemRequirement(material, amount));
        }

        return items;
    }

    // ============ Stages ============

    private List<QuestStage> parseStages(List<?> raw, String questId) {

        List<QuestStage> stages = new ArrayList<>();

        if (raw == null) {
            throw new IllegalArgumentException("la quest '" + questId + "' no define ningún 'stages'");
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }

            Object idObj = map.get("id");
            String stageId = idObj != null ? idObj.toString() : "stage-" + (stages.size() + 1);

            List<QuestObjective> objectives = parseObjectives(map.get("objectives"));
            List<String> conditions = parseStringList(map.get("conditions"));
            Dialog dialog = parseDialog(map.get("dialog"), map.get("options"));
            Map<QuestEventType, List<QuestAction>> events = parseEventsFromMap(map);

            stages.add(new QuestStage(stageId, objectives, conditions, dialog, events));
        }

        return stages;
    }

    private List<QuestObjective> parseObjectives(Object raw) {

        List<QuestObjective> objectives = new ArrayList<>();

        if (!(raw instanceof List<?> list)) {
            return objectives;
        }

        for (Object entry : list) {

            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }

            Object typeObj = map.get("type");
            if (typeObj == null) {
                continue;
            }

            String type = typeObj.toString().trim().toUpperCase(Locale.ROOT);
            String description = map.get("description") != null ? map.get("description").toString() : "";
            int amount = map.get("amount") != null ? parseInt(map.get("amount").toString(), 1) : 1;

            Map<String, String> params = new HashMap<>();

            for (var mapEntry : map.entrySet()) {

                String key = mapEntry.getKey().toString();

                if (key.equals("type") || key.equals("description") || key.equals("amount")
                        || mapEntry.getValue() == null) {
                    continue;
                }

                params.put(key, mapEntry.getValue().toString());
            }

            objectives.add(new QuestObjective(type, description, amount, params));
        }

        return objectives;
    }

    private Dialog parseDialog(Object rawDialog, Object rawOptions) {

        List<DialogLine> lines = new ArrayList<>();

        if (rawDialog instanceof List<?> list) {

            for (Object entry : list) {

                if (!(entry instanceof Map<?, ?> map)) {
                    continue;
                }

                if (map.get("npc") != null) {
                    lines.add(new DialogLine(DialogSpeaker.NPC, map.get("npc").toString().trim()));
                } else if (map.get("player") != null) {
                    lines.add(new DialogLine(DialogSpeaker.PLAYER, map.get("player").toString().trim()));
                }
            }
        }

        List<DialogOption> options = new ArrayList<>();

        if (rawOptions instanceof List<?> list) {

            for (Object entry : list) {

                if (!(entry instanceof Map<?, ?> map)) {
                    continue;
                }

                Object label = map.get("label");
                Object nextStage = map.get("next-stage");

                if (label == null || nextStage == null) {
                    continue;
                }

                options.add(new DialogOption(label.toString(), nextStage.toString()));
            }
        }

        if (lines.isEmpty() && options.isEmpty()) {
            return null;
        }

        return new Dialog(lines, options);
    }

    private Map<QuestEventType, List<QuestAction>> parseEventsFromMap(Map<?, ?> stageMap) {

        Map<QuestEventType, List<QuestAction>> events = new EnumMap<>(QuestEventType.class);

        for (QuestEventType type : QuestEventType.class.getEnumConstants()) {

            String yamlKey = "on-" + type.name().substring(3).toLowerCase(Locale.ROOT).replace('_', '-');
            Object raw = stageMap.get(yamlKey);

            if (raw instanceof List<?> list) {
                events.put(type, parseActions(list));
            }
        }

        return events;
    }

    private Map<QuestEventType, List<QuestAction>> parseEvents(ConfigurationSection section) {

        Map<QuestEventType, List<QuestAction>> events = new EnumMap<>(QuestEventType.class);

        if (section == null) {
            return events;
        }

        for (QuestEventType type : QuestEventType.class.getEnumConstants()) {

            String yamlKey = "on-" + type.name().substring(3).toLowerCase(Locale.ROOT).replace('_', '-');
            events.put(type, parseActions(section.getList(yamlKey)));
        }

        return events;
    }

    private List<QuestAction> parseActions(List<?> raw) {

        List<QuestAction> actions = new ArrayList<>();

        if (raw == null) {
            return actions;
        }

        for (Object entry : raw) {

            if (entry instanceof String simple) {
                actions.add(new QuestAction(simple.trim().toUpperCase(Locale.ROOT), Map.of()));
                continue;
            }

            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }

            Object typeObj = map.get("type");
            if (typeObj == null) {
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

            actions.add(new QuestAction(typeObj.toString().trim().toUpperCase(Locale.ROOT), params));
        }

        return actions;
    }

    // ============ Rewards ============

    private QuestRewards parseRewards(ConfigurationSection section) {

        if (section == null) {
            return QuestRewards.none();
        }

        double money = section.getDouble("money", 0);
        int experience = section.getInt("experience", 0);

        List<ItemRequirement> items = new ArrayList<>();
        for (String materialName : section.getStringList("items")) {
            Material material = parseMaterial(materialName);
            if (material != null) {
                items.add(new ItemRequirement(material, 1));
            }
        }

        List<String> commands = section.getStringList("commands");
        List<String> quests = section.getStringList("quests");
        List<QuestAction> extra = parseActions(section.getList("extra"));

        return new QuestRewards(money, experience, items, commands, quests, extra);
    }

    // ============ Helpers ============

    private List<String> parseStringList(Object raw) {

        if (raw instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object entry : list) {
                result.add(entry.toString());
            }
            return result;
        }

        return List.of();
    }

    private Material parseMaterial(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
