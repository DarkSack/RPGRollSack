package com.sack.rpgroll.guilds.guild.quest;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class GuildQuestDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public GuildQuestDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(GuildQuestDefinition definition) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", definition.id());
        config.set("display-name", definition.displayName());
        config.set("description", definition.description());
        config.set("type", definition.type().name());
        config.set("target-reference", definition.targetReference());
        config.set("target-amount", definition.targetAmount());
        config.set("reward-money", definition.rewardMoney());
        config.set("reward-xp", definition.rewardXp());
        config.set("min-guild-level", definition.minGuildLevel());

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, definition.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando quest de guild '" + definition.id() + "': " + e.getMessage());
        }
    }

}
