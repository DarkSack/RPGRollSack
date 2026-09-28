package com.sack.rpgroll.fishing.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class TreasureDefinitionWriter {

    private final File folder;

    public TreasureDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "treasures");
    }

    public void save(Treasure treasure) {

        File file = new File(folder, treasure.id() + ".yml");
        // Se parte del fichero que haya: así se conservan las claves que el editor no toca (model:...).
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        config.set("id", treasure.id());
        config.set("display-name", treasure.displayName());
        config.set("icon", treasure.icon());
        config.set("description", treasure.description());
        config.set("rarity", treasure.rarity().name());
        config.set("reward-material", treasure.rewardMaterial());
        config.set("reward-amount", treasure.rewardAmount());
        config.set("weight", treasure.weight());

        try {
            folder.mkdirs();
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el tesoro " + treasure.id(), e);
        }
    }

}
