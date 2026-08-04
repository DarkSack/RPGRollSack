package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

public class SchoolParser implements ContentParser<MagicSchool> {

    @Override
    public MagicSchool parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new MagicSchool(
                id,
                config.getString("display-name", id),
                config.getString("color", "WHITE"),
                config.getString("icon", "BOOK"),
                config.getString("description", ""),
                config.getString("cast-sound"),
                config.getString("cast-effect"),
                parseAffinities(config, "race-affinities"),
                parseAffinities(config, "class-affinities"));
    }

    private Map<String, Double> parseAffinities(YamlConfiguration config, String key) {

        var section = config.getConfigurationSection(key);

        if (section == null) {
            return Map.of();
        }

        Map<String, Double> result = new LinkedHashMap<>();

        for (String affinityKey : section.getKeys(false)) {
            result.put(affinityKey.toLowerCase(java.util.Locale.ROOT), section.getDouble(affinityKey));
        }

        return result;
    }

}
