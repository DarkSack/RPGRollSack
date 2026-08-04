package com.sack.rpgroll.dungeons.core;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.dungeons.util.DurationParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Convierte un YAML de dungeons/ en una {@link DungeonDefinition} completa. Solo "id" es obligatorio. */
public class DungeonParser implements ContentParser<DungeonDefinition> {

    @Override
    public DungeonDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String category = config.getString("category", "misc");
        String displayName = config.getString("display-name", id);
        String icon = config.getString("icon", "STONE_BRICKS");
        String description = config.getString("description", "");
        int recommendedLevel = config.getInt("recommended-level", 1);
        int estimatedMinutes = config.getInt("estimated-minutes", 15);
        int minPlayers = config.getInt("min-players", 1);
        int maxPlayers = config.getInt("max-players", 5);
        long cooldownMillis = DurationParser.parseMillis(config.getString("cooldown", ""));
        boolean repeatable = config.getBoolean("repeatable", true);
        List<String> tags = config.getStringList("tags");

        DungeonPoint lobbyPoint = parsePoint(config.getConfigurationSection("lobby"));
        DungeonBounds bounds = parseBounds(config.getConfigurationSection("bounds"));
        List<DungeonRoom> rooms = parseRooms(config.getList("rooms"));
        List<DungeonDifficulty> difficulties = parseDifficulties(config.getList("difficulties"));
        DungeonCheckpointPolicy checkpointPolicy = parseCheckpointPolicy(config.getConfigurationSection("checkpoints"));
        DungeonReviveConfig reviveConfig = parseRevive(config.getConfigurationSection("revive"));
        List<DungeonLootEntry> loot = parseLoot(config.getList("loot"));
        Map<DungeonTrigger, List<DungeonAction>> triggers = parseTriggers(config.getConfigurationSection("triggers"));

        return new DungeonDefinition(id, category, displayName, icon, description, recommendedLevel,
                estimatedMinutes, minPlayers, maxPlayers, cooldownMillis, repeatable, tags, lobbyPoint, bounds,
                rooms, difficulties, checkpointPolicy, reviveConfig, loot, triggers);
    }

    // ============ Helpers genéricos ============

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

    private DungeonPoint parsePoint(ConfigurationSection section) {

        if (section == null) {
            return new DungeonPoint("world", 0, 64, 0, 0, 0);
        }

        return new DungeonPoint(
                section.getString("world", "world"),
                section.getDouble("x", 0),
                section.getDouble("y", 64),
                section.getDouble("z", 0),
                (float) section.getDouble("yaw", 0),
                (float) section.getDouble("pitch", 0));
    }

    private DungeonBounds parseBounds(ConfigurationSection section) {

        if (section == null) {
            return DungeonBounds.none();
        }

        return new DungeonBounds(
                section.getString("world", "world"),
                section.getDouble("min-x", 0), section.getDouble("min-y", 0), section.getDouble("min-z", 0),
                section.getDouble("max-x", 0), section.getDouble("max-y", 0), section.getDouble("max-z", 0));
    }

    // ============ Rooms ============

    private List<DungeonRoom> parseRooms(List<?> raw) {

        List<DungeonRoom> rooms = new ArrayList<>();

        if (raw == null) {
            return rooms;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("id") == null) {
                continue;
            }

            String id = map.get("id").toString();
            DungeonRoomType type = parseEnum(map.get("type"), DungeonRoomType.class, DungeonRoomType.COMBAT);
            DungeonPoint entryPoint = parsePointFromMap(map.get("entry"));

            List<DungeonObjective> objectives = map.get("objectives") instanceof List<?> objectiveList
                    ? parseObjectives(objectiveList) : List.of();

            List<DungeonWave> waves = map.get("waves") instanceof List<?> waveList
                    ? parseWaves(waveList) : List.of();

            String bossMobId = map.get("boss") != null ? map.get("boss").toString() : null;

            Map<DungeonTrigger, List<DungeonAction>> events = map.get("events") instanceof Map<?, ?> eventsMap
                    ? parseTriggerMap(eventsMap) : Map.of();

            rooms.add(new DungeonRoom(id, type, entryPoint, objectives, waves, bossMobId, events));
        }

        return rooms;
    }

    @SuppressWarnings("unchecked")
    private DungeonPoint parsePointFromMap(Object raw) {

        if (!(raw instanceof Map<?, ?> map)) {
            return new DungeonPoint("world", 0, 64, 0, 0, 0);
        }

        Map<String, Object> section = (Map<String, Object>) map;

        return new DungeonPoint(
                stringOrDefault(section.get("world"), "world"),
                parseDouble(section.get("x"), 0),
                parseDouble(section.get("y"), 64),
                parseDouble(section.get("z"), 0),
                (float) parseDouble(section.get("yaw"), 0),
                (float) parseDouble(section.get("pitch"), 0));
    }

    private String stringOrDefault(Object raw, String fallback) {
        return raw != null ? raw.toString() : fallback;
    }

    // ============ Objectives ============

    private List<DungeonObjective> parseObjectives(List<?> raw) {

        List<DungeonObjective> objectives = new ArrayList<>();

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("type") == null) {
                continue;
            }

            DungeonObjectiveType type;
            try {
                type = DungeonObjectiveType.valueOf(map.get("type").toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                continue;
            }

            String description = map.get("description") != null ? map.get("description").toString() : null;
            int amount = map.get("amount") != null ? parseInt(map.get("amount"), 1) : 1;

            Map<String, String> params = new HashMap<>();
            for (var mapEntry : map.entrySet()) {

                String key = mapEntry.getKey().toString();
                if (key.equals("type") || key.equals("description") || key.equals("amount")
                        || mapEntry.getValue() == null) {
                    continue;
                }

                params.put(key, mapEntry.getValue().toString());
            }

            objectives.add(new DungeonObjective(type, description, amount, params));
        }

        return objectives;
    }

    // ============ Waves ============

    private List<DungeonWave> parseWaves(List<?> raw) {

        List<DungeonWave> waves = new ArrayList<>();

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("id") == null) {
                continue;
            }

            String id = map.get("id").toString();
            long timeLimitMillis = map.get("time-limit") != null
                    ? DurationParser.parseMillis(map.get("time-limit").toString()) : 0;
            long delayBeforeMillis = map.get("delay-before") != null
                    ? DurationParser.parseMillis(map.get("delay-before").toString()) : 0;

            List<DungeonWaveMob> mobs = new ArrayList<>();
            if (map.get("mobs") instanceof List<?> mobList) {
                for (Object mobEntry : mobList) {
                    if (mobEntry instanceof Map<?, ?> mobMap && mobMap.get("id") != null) {
                        int amount = mobMap.get("amount") != null ? parseInt(mobMap.get("amount"), 1) : 1;
                        mobs.add(new DungeonWaveMob(mobMap.get("id").toString(), amount));
                    }
                }
            }

            waves.add(new DungeonWave(id, mobs, timeLimitMillis, delayBeforeMillis));
        }

        return waves;
    }

    // ============ Difficulties ============

    private List<DungeonDifficulty> parseDifficulties(List<?> raw) {

        List<DungeonDifficulty> difficulties = new ArrayList<>();

        if (raw == null) {
            return difficulties;
        }

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("id") == null) {
                continue;
            }

            String id = map.get("id").toString();
            String displayName = map.get("display-name") != null ? map.get("display-name").toString() : id;
            double healthMultiplier = map.get("health-multiplier") != null
                    ? parseDouble(map.get("health-multiplier"), 1.0) : 1.0;
            double damageMultiplier = map.get("damage-multiplier") != null
                    ? parseDouble(map.get("damage-multiplier"), 1.0) : 1.0;
            double lootMultiplier = map.get("loot-multiplier") != null
                    ? parseDouble(map.get("loot-multiplier"), 1.0) : 1.0;

            List<DungeonModifierType> modifiers = new ArrayList<>();
            if (map.get("modifiers") instanceof List<?> modifierList) {
                for (Object modifierEntry : modifierList) {
                    try {
                        modifiers.add(DungeonModifierType.valueOf(
                                modifierEntry.toString().trim().toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            difficulties.add(new DungeonDifficulty(id, displayName, healthMultiplier, damageMultiplier,
                    lootMultiplier, modifiers));
        }

        return difficulties;
    }

    // ============ Checkpoints / Revive ============

    private DungeonCheckpointPolicy parseCheckpointPolicy(ConfigurationSection section) {

        if (section == null) {
            return DungeonCheckpointPolicy.defaultPolicy();
        }

        CheckpointMode mode = parseEnumString(section.getString("mode"), CheckpointMode.class,
                CheckpointMode.REVIVE_AT_CHECKPOINT);
        boolean sharedLives = section.getBoolean("shared-lives", false);
        int maxRetries = section.getInt("max-retries", -1);

        return new DungeonCheckpointPolicy(mode, sharedLives, maxRetries);
    }

    private DungeonReviveConfig parseRevive(ConfigurationSection section) {

        if (section == null) {
            return DungeonReviveConfig.none();
        }

        ReviveMode mode = parseEnumString(section.getString("mode"), ReviveMode.class, ReviveMode.NONE);
        long reviveTimerMillis = DurationParser.parseMillis(section.getString("timer", ""));
        String itemMaterial = section.getString("item");

        return new DungeonReviveConfig(mode, reviveTimerMillis, itemMaterial);
    }

    // ============ Loot ============

    private List<DungeonLootEntry> parseLoot(List<?> raw) {

        List<DungeonLootEntry> loot = new ArrayList<>();

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

            LootScope scope = LootScope.SHARED;
            if (map.get("scope") != null) {
                try {
                    scope = LootScope.valueOf(map.get("scope").toString().trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                }
            }

            loot.add(new DungeonLootEntry(type, reference, amountMin, amountMax, chance, scope));
        }

        return loot;
    }

    // ============ Triggers / Actions ============

    private List<DungeonAction> parseActions(List<?> raw) {

        List<DungeonAction> actions = new ArrayList<>();

        if (raw == null) {
            return actions;
        }

        for (Object entry : raw) {

            if (entry instanceof String simple) {
                actions.add(new DungeonAction(simple.trim().toUpperCase(Locale.ROOT), Map.of()));
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

            actions.add(new DungeonAction(map.get("type").toString().trim().toUpperCase(Locale.ROOT), params));
        }

        return actions;
    }

    private Map<DungeonTrigger, List<DungeonAction>> parseTriggers(ConfigurationSection section) {

        Map<DungeonTrigger, List<DungeonAction>> triggers = new EnumMap<>(DungeonTrigger.class);

        if (section == null) {
            return triggers;
        }

        for (String key : section.getKeys(false)) {

            try {
                DungeonTrigger trigger = DungeonTrigger.valueOf(key.trim().toUpperCase(Locale.ROOT));
                triggers.put(trigger, parseActions(section.getList(key)));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return triggers;
    }

    @SuppressWarnings("unchecked")
    private Map<DungeonTrigger, List<DungeonAction>> parseTriggerMap(Map<?, ?> rawMap) {

        Map<DungeonTrigger, List<DungeonAction>> triggers = new EnumMap<>(DungeonTrigger.class);
        Map<String, Object> map = (Map<String, Object>) rawMap;

        for (var entry : map.entrySet()) {

            try {
                DungeonTrigger trigger = DungeonTrigger.valueOf(entry.getKey().trim().toUpperCase(Locale.ROOT));

                if (entry.getValue() instanceof List<?> actionList) {
                    triggers.put(trigger, parseActions(actionList));
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return triggers;
    }

    private <T extends Enum<T>> T parseEnum(Object raw, Class<T> type, T fallback) {

        if (raw == null) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, raw.toString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private <T extends Enum<T>> T parseEnumString(String raw, Class<T> type, T fallback) {

        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

}
