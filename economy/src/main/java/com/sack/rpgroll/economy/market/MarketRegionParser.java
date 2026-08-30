package com.sack.rpgroll.economy.market;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

public class MarketRegionParser implements ContentParser<MarketRegion> {

    @Override
    public MarketRegion parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String world = config.getString("world");
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'world'");
        }

        ConfigurationSection bounds = config.getConfigurationSection("bounds");

        return new MarketRegion(
                id,
                config.getString("display-name", id),
                world,
                bounds == null ? 0 : bounds.getDouble("min-x"),
                bounds == null ? 0 : bounds.getDouble("min-y"),
                bounds == null ? 0 : bounds.getDouble("min-z"),
                bounds == null ? 0 : bounds.getDouble("max-x"),
                bounds == null ? 0 : bounds.getDouble("max-y"),
                bounds == null ? 0 : bounds.getDouble("max-z"),
                parseModifiers(config, "category-modifiers"),
                parseModifiers(config, "product-modifiers"));
    }

    private Map<String, Double> parseModifiers(YamlConfiguration config, String key) {

        ConfigurationSection section = config.getConfigurationSection(key);
        if (section == null) {
            return Map.of();
        }

        Map<String, Double> result = new LinkedHashMap<>();
        for (String modifierKey : section.getKeys(false)) {
            result.put(modifierKey, section.getDouble(modifierKey));
        }
        return result;
    }

}
