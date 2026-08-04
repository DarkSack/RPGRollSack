package com.sack.rpgroll.chat.emote;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class EmoteDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public EmoteDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(EmoteDefinition emote) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", emote.id());
        config.set("template", emote.template());
        config.set("target-template", emote.targetTemplate());
        config.set("radius", emote.radius());

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, emote.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando emote '" + emote.id() + "': " + e.getMessage());
        }
    }

}
