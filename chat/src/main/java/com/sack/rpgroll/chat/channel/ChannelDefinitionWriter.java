package com.sack.rpgroll.chat.channel;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/** Serializa un {@link ChatChannel} de vuelta a YAML — usado por el editor GUI. */
public class ChannelDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public ChannelDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(ChatChannel channel) {

        YamlConfiguration config = new YamlConfiguration();

        config.set("id", channel.id());
        config.set("display-name", channel.displayName());
        config.set("icon", channel.icon());
        config.set("color", channel.color());
        config.set("priority", channel.priority());
        config.set("scope", channel.scope().name());
        config.set("distance", channel.distance());
        config.set("view-permission", channel.viewPermission());
        config.set("speak-permission", channel.speakPermission());
        config.set("cooldown-millis", channel.cooldownMillis());
        config.set("format", channel.format());
        config.set("text-format", channel.textFormat().name());
        config.set("join-sound", channel.joinSound());
        config.set("filter-profanity", channel.filterProfanity());
        config.set("filter-caps", channel.filterCaps());
        config.set("allow-urls", channel.allowUrls());
        config.set("default-joined", channel.defaultJoined());
        config.set("cross-world", channel.crossWorld());
        config.set("also-action-bar", channel.alsoActionBar());

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, channel.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando canal '" + channel.id() + "': " + e.getMessage());
        }
    }

}
