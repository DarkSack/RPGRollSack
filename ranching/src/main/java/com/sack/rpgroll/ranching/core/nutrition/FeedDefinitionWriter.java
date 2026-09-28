package com.sack.rpgroll.ranching.core.nutrition;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FeedDefinitionWriter {

    private final File folder;

    public FeedDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "feeds");
    }

    public void save(Feed feed) {

        File file = new File(folder, feed.id() + ".yml");
        // Se parte del fichero que haya: así se conservan las claves que el editor no toca (model:...).
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        config.set("id", feed.id());
        config.set("display-name", feed.displayName());
        config.set("icon", feed.icon());
        config.set("description", feed.description());
        config.set("quality", feed.quality().name());
        config.set("tags", List.copyOf(feed.tags()));
        config.set("nutrition-value", feed.nutritionValue());
        config.set("health-bonus", feed.healthBonus());
        config.set("happiness-bonus", feed.happinessBonus());
        config.set("production-bonus", feed.productionBonus());

        try {
            folder.mkdirs();
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el alimento " + feed.id(), e);
        }
    }

}
