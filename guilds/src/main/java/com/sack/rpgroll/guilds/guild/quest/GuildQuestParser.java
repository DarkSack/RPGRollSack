package com.sack.rpgroll.guilds.guild.quest;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;

public class GuildQuestParser implements ContentParser<GuildQuestDefinition> {

    @Override
    public GuildQuestDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Falta 'id'");
        }

        GuildQuestType type = GuildQuestType.valueOf(
                config.getString("type", "GATHER_RESOURCE").toUpperCase(Locale.ROOT));

        return new GuildQuestDefinition(
                id,
                config.getString("display-name", id),
                config.getString("description", ""),
                type,
                config.getString("target-reference", null),
                config.getInt("target-amount", 1),
                config.getDouble("reward-money", 0),
                config.getInt("reward-xp", 0),
                config.getInt("min-guild-level", 1));
    }

}
