package com.sack.rpgroll.tab.bossbar;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class BossBarParser implements ContentParser<BossBarDefinition> {

    @Override
    public BossBarDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new BossBarDefinition(
                id,
                config.getString("title", id),
                config.getString("progress", "100"),
                config.getString("color", "PURPLE"),
                config.getString("style", "PROGRESS"),
                config.getInt("priority", 0));
    }

}
