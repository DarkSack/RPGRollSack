package com.sack.rpgroll.items.socket;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GemParser implements ContentParser<Gem> {

    @Override
    public Gem parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String type = config.getString("type", "GENERIC");

        Map<String, Double> statBonus = new HashMap<>();
        ConfigurationSection statsSection = config.getConfigurationSection("stats");

        if (statsSection != null) {
            for (String key : statsSection.getKeys(false)) {
                if (statsSection.get(key) instanceof Number number) {
                    statBonus.put(key.toLowerCase(Locale.ROOT), number.doubleValue());
                }
            }
        }

        org.bukkit.Material material = null;
        String rawMaterial = config.getString("material");
        if (rawMaterial != null && !rawMaterial.isBlank()) {
            try {
                material = org.bukkit.Material.valueOf(rawMaterial.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("gema '" + id + "' tiene un 'material' inválido: " + rawMaterial);
            }
        }

        String itemModel = com.sack.rpgroll.items.core.ItemParser.parseItemModel(config.getString("item-model"), id);

        return new Gem(id, displayName, type, statBonus, material, itemModel);
    }

}
