package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class CatalystParser implements ContentParser<SpellCatalyst> {

    @Override
    public SpellCatalyst parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new SpellCatalyst(
                id,
                config.getString("display-name", id),
                config.getString("material", "BLAZE_ROD"),
                config.getString("description", ""),
                config.getDouble("power-multiplier", 1.0),
                config.getDouble("cost-multiplier", 1.0),
                config.getDouble("range-multiplier", 1.0));
    }

}
