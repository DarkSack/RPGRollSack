package com.sack.rpgroll.ascension.deferred;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class AchievementDefinitionWriter {

    private final File folder;

    public AchievementDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "achievements");
    }

    public void save(Achievement achievement) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", achievement.id());
        config.set("display-name", achievement.displayName());
        config.set("description", achievement.description());

        try {
            folder.mkdirs();
            config.save(new File(folder, achievement.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el logro " + achievement.id(), e);
        }
    }

}
