package com.sack.rpgroll.tab.nametag;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class NametagParser implements ContentParser<NametagDefinition> {

    @Override
    public NametagDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new NametagDefinition(
                id,
                config.getStringList("lines"),
                config.getString("staff-override.permission"),
                config.getStringList("staff-override.lines"));
    }

}
