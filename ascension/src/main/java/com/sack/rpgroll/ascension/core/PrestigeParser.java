package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class PrestigeParser implements ContentParser<PrestigeLevel> {

    @Override
    public PrestigeLevel parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        int requiredLevel = config.getInt("required-level", 100);
        double expBonusPercent = config.getDouble("exp-bonus-percent", 0);

        return new PrestigeLevel(id, requiredLevel, expBonusPercent, config.getStringList("skills"));
    }

}
