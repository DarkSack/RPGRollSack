package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FishingRodParser implements ContentParser<FishingRod> {

    @Override
    public FishingRod parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        Set<String> categories = new HashSet<>();
        for (String entry : config.getStringList("preferred-categories")) {
            categories.add(entry.trim().toLowerCase(Locale.ROOT));
        }

        return new FishingRod(
                id,
                config.getString("display-name", id),
                config.getString("material", "FISHING_ROD"),
                config.getString("description", ""),
                config.getInt("durability", 64),
                config.getDouble("cast-power", 1.0),
                config.getDouble("reel-speed", 1.0),
                config.getDouble("precision", 0),
                config.getDouble("resistance", 1.0),
                config.getDouble("luck-bonus", 1.0),
                categories);
    }

}
