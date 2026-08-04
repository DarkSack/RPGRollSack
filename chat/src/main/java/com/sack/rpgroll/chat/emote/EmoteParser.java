package com.sack.rpgroll.chat.emote;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class EmoteParser implements ContentParser<EmoteDefinition> {

    @Override
    public EmoteDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new EmoteDefinition(
                id,
                config.getString("template", "{player} hace una acción."),
                config.getString("target-template", null),
                config.getDouble("radius", 0));
    }

}
