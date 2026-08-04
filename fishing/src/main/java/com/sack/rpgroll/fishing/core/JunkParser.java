package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class JunkParser implements ContentParser<Junk> {

    @Override
    public Junk parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new Junk(id, config.getString("display-name", id), config.getString("icon", "LEATHER_BOOTS"),
                config.getString("description", ""), config.getDouble("weight", 1.0));
    }

}
