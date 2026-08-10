package com.sack.rpgroll.tab.belowname;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class BelowNameParser implements ContentParser<BelowNameDefinition> {

    @Override
    public BelowNameDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String score = config.getString("score", "{health}");
        String label = config.getString("label", "");

        return new BelowNameDefinition(id, score, label);
    }

}
