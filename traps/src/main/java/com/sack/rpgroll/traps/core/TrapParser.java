package com.sack.rpgroll.traps.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrapParser implements ContentParser<TrapDefinition> {

    @Override
    public TrapDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String description = config.getString("description", "");
        Material icon = parseMaterial(config.getString("icon"), Material.TRIPWIRE_HOOK);

        TrapTrigger trigger = parseTrigger(config.getString("trigger.type"));
        Map<String, String> triggerParams = parseStringMap(config.getConfigurationSection("trigger.params"));

        double radius = config.getDouble("radius", 1.5);
        List<String> conditions = config.getStringList("conditions");
        List<TrapAction> actions = parseActions(config, "actions");
        long cooldownMillis = config.getLong("cooldown-millis", 0);
        int charges = config.getInt("charges", -1);
        List<String> chain = config.getStringList("chain");

        TrapBlockConfig block = parseBlock(config.getConfigurationSection("block"));
        TrapDisguiseConfig disguise = parseDisguise(config.getConfigurationSection("disguise"));

        return new TrapDefinition(id, displayName, description, icon, trigger, triggerParams, radius, conditions,
                actions, cooldownMillis, charges, chain, block, disguise);
    }

    private TrapTrigger parseTrigger(String raw) {
        try {
            return TrapTrigger.valueOf((raw == null ? "PRESSURE" : raw).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("trigger.type inválido: " + raw);
        }
    }

    private Material parseMaterial(String raw, Material fallback) {

        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private Map<String, String> parseStringMap(ConfigurationSection section) {

        Map<String, String> map = new LinkedHashMap<>();

        if (section == null) {
            return map;
        }

        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value != null) {
                map.put(key, value.toString());
            }
        }

        return map;
    }

    private List<TrapAction> parseActions(YamlConfiguration config, String path) {

        List<TrapAction> actions = new ArrayList<>();
        List<Map<?, ?>> raw = config.getMapList(path);

        for (Map<?, ?> entry : raw) {

            Object typeObj = entry.get("type");
            if (typeObj == null) {
                continue;
            }

            Map<String, String> params = new LinkedHashMap<>();
            if (entry.get("params") instanceof Map<?, ?> rawParams) {
                for (var kv : rawParams.entrySet()) {
                    params.put(kv.getKey().toString(), kv.getValue() == null ? "" : kv.getValue().toString());
                }
            }

            actions.add(new TrapAction(typeObj.toString().toUpperCase(Locale.ROOT), params));
        }

        return actions;
    }

    private TrapBlockConfig parseBlock(ConfigurationSection section) {

        if (section == null) {
            return TrapBlockConfig.none();
        }

        Material material = parseMaterial(section.getString("material"), null);
        boolean breakable = section.getBoolean("breakable", true);
        String requiredItemId = section.getString("required-item-id");
        int breakTimeSeconds = section.getInt("break-time-seconds", 0);
        boolean explosionImmune = section.getBoolean("explosion-immune", false);
        boolean pistonImmune = section.getBoolean("piston-immune", false);

        return new TrapBlockConfig(material, breakable, requiredItemId, breakTimeSeconds, explosionImmune,
                pistonImmune);
    }

    private TrapDisguiseConfig parseDisguise(ConfigurationSection section) {

        if (section == null) {
            return TrapDisguiseConfig.none();
        }

        TrapDisguiseConfig.DisguiseMode mode;
        try {
            mode = TrapDisguiseConfig.DisguiseMode.valueOf(
                    section.getString("mode", "NONE").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            mode = TrapDisguiseConfig.DisguiseMode.NONE;
        }

        Material visible = parseMaterial(section.getString("visible-material"), null);
        Material revealed = parseMaterial(section.getString("revealed-material"), null);

        return new TrapDisguiseConfig(mode, visible, revealed);
    }

}
