package com.sack.rpgroll.tab.profile;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class ProfileParser implements ContentParser<TABProfile> {

    @Override
    public TABProfile parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new TABProfile(
                id,
                config.getString("tablist"),
                config.getString("scoreboard"),
                config.getString("nametag"),
                config.getString("belowname"),
                config.getString("bossbar"),
                config.getString("layout"),
                config.getString("sorting"),
                config.getString("teams"));
    }

}
