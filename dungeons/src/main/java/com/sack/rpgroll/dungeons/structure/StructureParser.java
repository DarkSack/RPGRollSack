package com.sack.rpgroll.dungeons.structure;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StructureParser implements ContentParser<StructureDefinition> {

    @Override
    public StructureDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String description = config.getString("description", "");
        Material icon = parseMaterial(config.getString("icon"), Material.STONE_BRICKS);
        StructureSourceType sourceType = parseSourceType(config.getString("source", "CUSTOM"));

        if (sourceType == StructureSourceType.NATIVE || sourceType == StructureSourceType.SCHEMATIC) {
            return new StructureDefinition(id, displayName, description, icon, sourceType,
                    0, 0, 0, 0, 0, 0, Map.of(), List.of());
        }

        Map<Character, Material> palette = parsePalette(config.getConfigurationSection("palette"));
        List<List<String>> layers = parseLayers(config.getList("layers"));

        int height = layers.size();
        int depth = layers.isEmpty() ? 0 : layers.get(0).size();
        int width = (layers.isEmpty() || layers.get(0).isEmpty()) ? 0 : layers.get(0).get(0).length();

        ConfigurationSection anchor = config.getConfigurationSection("anchor");
        int anchorX = anchor != null ? anchor.getInt("x", 0) : 0;
        int anchorY = anchor != null ? anchor.getInt("y", 0) : 0;
        int anchorZ = anchor != null ? anchor.getInt("z", 0) : 0;

        return new StructureDefinition(id, displayName, description, icon, sourceType,
                width, height, depth, anchorX, anchorY, anchorZ, palette, layers);
    }

    private StructureSourceType parseSourceType(String raw) {
        try {
            return StructureSourceType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return StructureSourceType.CUSTOM;
        }
    }

    private Material parseMaterial(String raw, Material fallback) {

        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        Material material = Material.matchMaterial(raw);
        return material != null ? material : fallback;
    }

    /** '.' y ' ' son AIR por convención, aunque no aparezcan en la paleta del YAML. */
    private Map<Character, Material> parsePalette(ConfigurationSection section) {

        Map<Character, Material> palette = new LinkedHashMap<>();
        palette.put('.', Material.AIR);
        palette.put(' ', Material.AIR);

        if (section == null) {
            return palette;
        }

        for (String key : section.getKeys(false)) {

            if (key.isEmpty()) {
                continue;
            }

            Material material = parseMaterial(section.getString(key), null);

            if (material == null) {
                continue;
            }

            palette.put(key.charAt(0), material);
        }

        return palette;
    }

    private List<List<String>> parseLayers(List<?> raw) {

        List<List<String>> layers = new ArrayList<>();

        if (raw == null) {
            return layers;
        }

        for (Object layerObj : raw) {

            if (!(layerObj instanceof List<?> rowsRaw)) {
                continue;
            }

            List<String> rows = new ArrayList<>();

            for (Object rowObj : rowsRaw) {
                if (rowObj instanceof String row) {
                    rows.add(row);
                }
            }

            layers.add(rows);
        }

        return layers;
    }

}
