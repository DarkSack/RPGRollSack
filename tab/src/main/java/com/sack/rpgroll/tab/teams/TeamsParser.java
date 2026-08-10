package com.sack.rpgroll.tab.teams;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class TeamsParser implements ContentParser<TeamsDefinition> {

    @Override
    public TeamsDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new TeamsDefinition(
                id,
                config.getString("prefix", ""),
                config.getString("suffix", ""),
                config.getString("color"),
                config.getBoolean("friendly-fire", true),
                config.getBoolean("see-friendly-invisibles", true),
                config.getString("collision", "ALWAYS"),
                config.getString("nametag-visibility", "ALWAYS"));
    }

}
