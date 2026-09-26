package com.sack.rpgroll.items.ore;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lee {@code ores/<id>.yml}. Los estados de bloque se guardan como texto y
 * los resuelve {@link OreService} al cargar, porque crear un BlockData
 * necesita el servidor.
 */
public class OreParser implements ContentParser<OreDefinition> {

    @Override
    public OreDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("mena sin 'id'");
        }

        Map<String, String> blocks = new LinkedHashMap<>();
        ConfigurationSection blockSection = config.getConfigurationSection("blocks");
        if (blockSection != null) {
            for (String variant : blockSection.getKeys(false)) {
                String state = blockSection.getString(variant);
                if (state != null && !state.isBlank()) {
                    blocks.put(variant.toLowerCase(Locale.ROOT), state.trim());
                }
            }
        }
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("mena '" + id + "' sin 'blocks'");
        }

        String drop = config.getString("drop.item");
        if (drop == null || drop.isBlank()) {
            throw new IllegalArgumentException("mena '" + id + "' sin 'drop.item'");
        }

        int dropMin = Math.max(1, config.getInt("drop.min", 1));
        int dropMax = Math.max(dropMin, config.getInt("drop.max", dropMin));

        List<Integer> xp = config.getIntegerList("xp");
        int xpMin = xp.isEmpty() ? 0 : Math.max(0, xp.get(0));
        int xpMax = xp.size() < 2 ? xpMin : Math.max(xpMin, xp.get(1));

        double hardness = config.getDouble("hardness", 3.0);
        if (hardness <= 0) {
            throw new IllegalArgumentException("mena '" + id + "': 'hardness' tiene que ser mayor que 0");
        }

        List<OreDefinition.OreGeneration> generation = new ArrayList<>();
        for (Map<?, ?> raw : config.getMapList("generation")) {
            generation.add(parseGeneration(id, raw, blocks));
        }

        return new OreDefinition(id, config.getString("display-name", id), blocks, drop.trim(), dropMin, dropMax,
                config.getBoolean("drop.fortune", true), xpMin, xpMax, hardness,
                Math.max(0, config.getInt("required-tier", 0)), generation);
    }

    private static OreDefinition.OreGeneration parseGeneration(String id, Map<?, ?> raw, Map<String, String> blocks) {

        List<String> worlds = new ArrayList<>();
        if (raw.get("worlds") instanceof List<?> list) {
            list.forEach(world -> worlds.add(world.toString()));
        }
        if (worlds.isEmpty()) {
            throw new IllegalArgumentException("mena '" + id + "': una generación sin 'worlds'");
        }

        int minY = number(raw.get("min-y"), -64);
        int maxY = number(raw.get("max-y"), 64);
        if (maxY < minY) {
            throw new IllegalArgumentException("mena '" + id + "': 'max-y' menor que 'min-y'");
        }

        double veins = raw.get("veins-per-chunk") != null ? Double.parseDouble(raw.get("veins-per-chunk").toString())
                : 1.0;

        int veinMin = 1;
        int veinMax = 1;
        if (raw.get("vein-size") instanceof List<?> size && !size.isEmpty()) {
            veinMin = Math.max(1, number(size.get(0), 1));
            veinMax = Math.max(veinMin, number(size.get(size.size() - 1), veinMin));
        }

        Map<String, String> replace = new LinkedHashMap<>();
        if (raw.get("replace") instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                String variant = entry.getValue().toString().toLowerCase(Locale.ROOT);
                if (!blocks.containsKey(variant)) {
                    throw new IllegalArgumentException("mena '" + id + "': 'replace' usa la variante '" + variant
                            + "', que no está en 'blocks'");
                }
                replace.put(entry.getKey().toString().toUpperCase(Locale.ROOT), variant);
            }
        }
        if (replace.isEmpty()) {
            throw new IllegalArgumentException("mena '" + id + "': una generación sin 'replace'");
        }

        return new OreDefinition.OreGeneration(worlds, minY, maxY, Math.max(0, veins), veinMin, veinMax, replace);
    }

    private static int number(Object raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return (int) Math.round(Double.parseDouble(raw.toString()));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
