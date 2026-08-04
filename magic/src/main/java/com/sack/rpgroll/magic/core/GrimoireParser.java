package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class GrimoireParser implements ContentParser<Grimoire> {

    @Override
    public Grimoire parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new Grimoire(
                id,
                config.getString("display-name", id),
                config.getString("icon", "WRITTEN_BOOK"),
                config.getString("description", ""),
                config.getString("school"),
                config.getInt("required-level", 0),
                config.getStringList("spells"));
    }

}
