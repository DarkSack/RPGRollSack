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

        // Se escribe encima del fichero que ya hay, no se rehace: el editor
        // solo conoce estos campos, y criterios, rangos o recompensas se
        // escriben a mano en el YAML. Rehacerlo los borraba al renombrar.
        File file = new File(folder, achievement.id() + ".yml");
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        config.set("id", achievement.id());
        config.set("display-name", achievement.displayName());
        config.set("description", achievement.description());

        try {
            folder.mkdirs();
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el logro " + achievement.id(), e);
        }
    }

}
