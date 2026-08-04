package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class BaitParser implements ContentParser<Bait> {

    @Override
    public Bait parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        Set<String> tags = new HashSet<>();
        for (String entry : config.getStringList("tags")) {
            tags.add(entry.trim().toLowerCase(Locale.ROOT));
        }

        return new Bait(
                id,
                config.getString("display-name", id),
                config.getString("material", "STRING"),
                config.getString("description", ""),
                tags,
                config.getDouble("quality-bonus", 0),
                config.getDouble("legendary-weight-multiplier", 1.0));
    }

}
