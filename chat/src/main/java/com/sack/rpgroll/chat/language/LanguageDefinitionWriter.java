package com.sack.rpgroll.chat.language;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class LanguageDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public LanguageDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(Language language) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", language.id());
        config.set("display-name", language.displayName());
        config.set("obfuscation-char", String.valueOf(language.obfuscationChar()));
        config.set("default-for-races", language.defaultForRaces());

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, language.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando idioma '" + language.id() + "': " + e.getMessage());
        }
    }

}
