package com.sack.rpgroll.ranching.core.nutrition;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FeedParser implements ContentParser<Feed> {

    @Override
    public Feed parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new Feed(
                id,
                config.getString("display-name", id),
                config.getString("icon", "WHEAT"),
                config.getString("description", ""),
                parseQuality(config.getString("quality")),
                lowercaseSet(config.getStringList("tags")),
                config.getDouble("nutrition-value", 20),
                config.getDouble("health-bonus", 0),
                config.getDouble("happiness-bonus", 0),
                config.getDouble("production-bonus", 0));
    }

    private FeedQuality parseQuality(String raw) {

        if (raw == null || raw.isBlank()) {
            return FeedQuality.COMMON;
        }

        try {
            return FeedQuality.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return FeedQuality.COMMON;
        }
    }

    private Set<String> lowercaseSet(java.util.List<String> raw) {

        Set<String> result = new HashSet<>();

        for (String entry : raw) {
            result.add(entry.trim().toLowerCase(Locale.ROOT));
        }

        return result;
    }

}
