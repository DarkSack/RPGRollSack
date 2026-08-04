package com.sack.rpgroll.playerclass;

import com.sack.rpgroll.api.playerclass.PlayerClass;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/** Serializa una {@link PlayerClass} completa de vuelta a YAML — inverso de {@link ClassParser}. */
public class ClassDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public ClassDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(PlayerClass playerClass) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", playerClass.id());
        config.set("display-name", playerClass.displayName());
        config.set("description", playerClass.description());
        config.set("icon", playerClass.icon());
        config.set("passive-traits", playerClass.passiveTraits());
        config.set("lore", playerClass.lore());

        for (var entry : playerClass.baseAttributes().entrySet()) {
            config.set("base-attributes." + entry.getKey().name().toLowerCase(), entry.getValue());
        }

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, playerClass.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando clase '" + playerClass.id() + "': " + e.getMessage());
        }
    }

}
