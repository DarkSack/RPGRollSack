package com.sack.rpgroll.ranching.core.breeds;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class BreedDefinitionWriter {

    private final File folder;

    public BreedDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "breeds");
    }

    public void save(Breed breed) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", breed.id());
        config.set("display-name", breed.displayName());
        config.set("description", breed.description());
        config.set("species", breed.speciesId());
        config.set("production-multiplier", breed.productionMultiplier());
        config.set("weight-multiplier", breed.weightMultiplier());
        config.set("fertility-multiplier", breed.fertilityMultiplier());
        config.set("resistance-multiplier", breed.resistanceMultiplier());
        config.set("temperament", breed.temperament());

        try {
            folder.mkdirs();
            config.save(new File(folder, breed.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la raza " + breed.id(), e);
        }
    }

}
