package com.sack.rpgroll.dungeons.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Serializa una {@link DungeonDefinition} completa de vuelta a YAML — inverso de {@link DungeonParser}. */
public class DungeonDefinitionWriter {

    public void save(DungeonDefinition definition, File file) throws IOException {

        YamlConfiguration config = new YamlConfiguration();

        config.set("id", definition.id());
        config.set("category", definition.category());
        config.set("display-name", definition.displayName());
        config.set("icon", definition.icon());
        config.set("description", definition.description());
        config.set("recommended-level", definition.recommendedLevel());
        config.set("estimated-minutes", definition.estimatedMinutes());
        config.set("min-players", definition.minPlayers());
        config.set("max-players", definition.maxPlayers());
        config.set("cooldown", (definition.cooldownMillis() / 1000) + "s");
        config.set("repeatable", definition.repeatable());
        config.set("tags", definition.tags());

        writePoint(config, "lobby", definition.lobbyPoint());
        writeBounds(config, definition.bounds());
        writeRooms(config, definition.rooms());
        writeDifficulties(config, definition.difficulties());
        writeCheckpointPolicy(config, definition.checkpointPolicy());
        writeRevive(config, definition.reviveConfig());
        writeLoot(config, definition.loot());
        writeTriggers(config, definition.triggers());

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        config.save(file);
    }

    private void writePoint(YamlConfiguration config, String path, DungeonPoint point) {
        config.set(path + ".world", point.world());
        config.set(path + ".x", point.x());
        config.set(path + ".y", point.y());
        config.set(path + ".z", point.z());
        config.set(path + ".yaw", point.yaw());
        config.set(path + ".pitch", point.pitch());
    }

    private void writeBounds(YamlConfiguration config, DungeonBounds bounds) {
        config.set("bounds.world", bounds.world());
        config.set("bounds.min-x", bounds.minX());
        config.set("bounds.min-y", bounds.minY());
        config.set("bounds.min-z", bounds.minZ());
        config.set("bounds.max-x", bounds.maxX());
        config.set("bounds.max-y", bounds.maxY());
        config.set("bounds.max-z", bounds.maxZ());
    }

    private void writeRooms(YamlConfiguration config, List<DungeonRoom> rooms) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (DungeonRoom room : rooms) {

            Map<String, Object> entry = new HashMap<>();
            entry.put("id", room.id());
            entry.put("type", room.type().name());

            Map<String, Object> entryPoint = new HashMap<>();
            entryPoint.put("world", room.entryPoint().world());
            entryPoint.put("x", room.entryPoint().x());
            entryPoint.put("y", room.entryPoint().y());
            entryPoint.put("z", room.entryPoint().z());
            entryPoint.put("yaw", room.entryPoint().yaw());
            entryPoint.put("pitch", room.entryPoint().pitch());
            entry.put("entry", entryPoint);

            if (room.hasBoss()) {
                entry.put("boss", room.bossMobId());
            }

            entry.put("objectives", objectivesToRaw(room.objectives()));
            entry.put("waves", wavesToRaw(room.waves()));

            Map<String, Object> events = new HashMap<>();
            for (var eventEntry : room.events().entrySet()) {
                if (!eventEntry.getValue().isEmpty()) {
                    events.put(eventEntry.getKey().name(), actionsToRaw(eventEntry.getValue()));
                }
            }
            if (!events.isEmpty()) {
                entry.put("events", events);
            }

            raw.add(entry);
        }

        config.set("rooms", raw);
    }

    private List<Map<String, Object>> objectivesToRaw(List<DungeonObjective> objectives) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (DungeonObjective objective : objectives) {
            Map<String, Object> entry = new HashMap<>(objective.params());
            entry.put("type", objective.type().name());
            entry.put("description", objective.description());
            entry.put("amount", objective.amount());
            raw.add(entry);
        }

        return raw;
    }

    private List<Map<String, Object>> wavesToRaw(List<DungeonWave> waves) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (DungeonWave wave : waves) {

            Map<String, Object> entry = new HashMap<>();
            entry.put("id", wave.id());
            entry.put("time-limit", (wave.timeLimitMillis() / 1000) + "s");
            entry.put("delay-before", (wave.delayBeforeMillis() / 1000) + "s");

            List<Map<String, Object>> mobs = new ArrayList<>();
            for (DungeonWaveMob mob : wave.mobs()) {
                Map<String, Object> mobEntry = new HashMap<>();
                mobEntry.put("id", mob.mobId());
                mobEntry.put("amount", mob.amount());
                mobs.add(mobEntry);
            }
            entry.put("mobs", mobs);

            raw.add(entry);
        }

        return raw;
    }

    private void writeDifficulties(YamlConfiguration config, List<DungeonDifficulty> difficulties) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (DungeonDifficulty difficulty : difficulties) {

            Map<String, Object> entry = new HashMap<>();
            entry.put("id", difficulty.id());
            entry.put("display-name", difficulty.displayName());
            entry.put("health-multiplier", difficulty.healthMultiplier());
            entry.put("damage-multiplier", difficulty.damageMultiplier());
            entry.put("loot-multiplier", difficulty.lootMultiplier());
            entry.put("modifiers", difficulty.modifiers().stream().map(Enum::name).toList());

            raw.add(entry);
        }

        config.set("difficulties", raw);
    }

    private void writeCheckpointPolicy(YamlConfiguration config, DungeonCheckpointPolicy policy) {
        config.set("checkpoints.mode", policy.mode().name());
        config.set("checkpoints.shared-lives", policy.sharedLives());
        config.set("checkpoints.max-retries", policy.maxRetries());
    }

    private void writeRevive(YamlConfiguration config, DungeonReviveConfig revive) {
        config.set("revive.mode", revive.mode().name());
        config.set("revive.timer", (revive.reviveTimerMillis() / 1000) + "s");
        if (revive.itemMaterial() != null) {
            config.set("revive.item", revive.itemMaterial());
        }
    }

    private void writeLoot(YamlConfiguration config, List<DungeonLootEntry> loot) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (DungeonLootEntry entry : loot) {

            Map<String, Object> map = new HashMap<>();
            map.put("type", entry.type().name());

            if (entry.reference() != null) {
                map.put("reference", entry.reference());
            }

            map.put("amount-min", entry.amountMin());
            map.put("amount-max", entry.amountMax());
            map.put("chance", entry.chance());
            map.put("scope", entry.scope().name());

            raw.add(map);
        }

        config.set("loot", raw);
    }

    private void writeTriggers(YamlConfiguration config, Map<DungeonTrigger, List<DungeonAction>> triggers) {

        for (var entry : triggers.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                config.set("triggers." + entry.getKey().name(), actionsToRaw(entry.getValue()));
            }
        }
    }

    private List<Map<String, Object>> actionsToRaw(List<DungeonAction> actions) {

        List<Map<String, Object>> raw = new ArrayList<>();

        for (DungeonAction action : actions) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("type", action.type());
            entry.putAll(action.params());
            raw.add(entry);
        }

        return raw;
    }

}
