package com.sack.rpgroll.ranching.core.health;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DiseaseParser implements ContentParser<Disease> {

    @Override
    public Disease parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new Disease(
                id,
                config.getString("display-name", id),
                config.getString("description", ""),
                lowercaseSet(config.getStringList("symptoms")),
                config.getLong("duration-ticks", 6000L),
                config.getDouble("contagion-chance", 0.05),
                config.getDouble("health-penalty-per-check", 2.0),
                config.getDouble("happiness-penalty-per-check", 2.0),
                config.getDouble("production-penalty", 0.5));
    }

    private Set<String> lowercaseSet(java.util.List<String> raw) {

        Set<String> result = new HashSet<>();

        for (String entry : raw) {
            result.add(entry.trim().toLowerCase(Locale.ROOT));
        }

        return result;
    }

}
