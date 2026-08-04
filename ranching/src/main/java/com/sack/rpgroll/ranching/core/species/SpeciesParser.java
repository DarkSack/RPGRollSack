package com.sack.rpgroll.ranching.core.species;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SpeciesParser implements ContentParser<Species> {

    @Override
    public Species parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new Species(
                id,
                config.getString("display-name", id),
                config.getString("icon", "COW_SPAWN_EGG"),
                config.getString("description", ""),
                config.getString("entity-type", "COW"),
                lowercaseSet(config.getStringList("product-types")),
                parseBaseProduction(config),
                config.getDouble("base-weight-min", 100),
                config.getDouble("base-weight-max", 300),
                config.getLong("baby-stage-duration-ticks", 6000L),
                config.getLong("juvenile-stage-duration-ticks", 12000L),
                config.getLong("elder-threshold-ticks", 240000L),
                config.getLong("gestation-duration-ticks", 24000L),
                config.getInt("min-litter-size", 1),
                config.getInt("max-litter-size", 1),
                config.getDouble("base-fertility", 0.5),
                lowercaseSet(config.getStringList("diet-tags")));
    }

    private Map<String, Double> parseBaseProduction(YamlConfiguration config) {

        ConfigurationSection section = config.getConfigurationSection("base-production");

        if (section == null) {
            return Map.of();
        }

        Map<String, Double> result = new LinkedHashMap<>();

        for (String key : section.getKeys(false)) {
            result.put(key.toLowerCase(Locale.ROOT), section.getDouble(key));
        }

        return result;
    }

    private Set<String> lowercaseSet(java.util.List<String> raw) {

        Set<String> result = new HashSet<>();

        for (String entry : raw) {
            result.add(entry.trim().toLowerCase(Locale.ROOT));
        }

        return result;
    }

}
