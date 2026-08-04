package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class LegacyParser implements ContentParser<LegacyTier> {

    @Override
    public LegacyTier parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new LegacyTier(id, config.getInt("required-prestige", 1),
                config.getDouble("permanent-exp-bonus-percent", 5), config.getInt("bonus-stat-points", 0));
    }

}
