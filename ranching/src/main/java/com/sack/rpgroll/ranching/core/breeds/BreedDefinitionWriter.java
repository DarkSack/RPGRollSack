package com.sack.rpgroll.ranching.core.breeds;

import com.sack.rpgroll.common.reskin.EntityReskinYaml;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class BreedDefinitionWriter {

    private final File folder;

    public BreedDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "breeds");
    }

    public void save(Breed breed) {

        File file = new File(folder, breed.id() + ".yml");
        // Se parte del fichero que haya: así se conservan las claves que el editor no toca (model:...).
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        config.set("id", breed.id());
        config.set("display-name", breed.displayName());
        config.set("description", breed.description());
        config.set("species", breed.speciesId());
        config.set("production-multiplier", breed.productionMultiplier());
        config.set("weight-multiplier", breed.weightMultiplier());
        config.set("fertility-multiplier", breed.fertilityMultiplier());
        config.set("resistance-multiplier", breed.resistanceMultiplier());
        config.set("temperament", breed.temperament());
        EntityReskinYaml.write(config, breed.reskin());
        if (breed.model() != null) {
            config.set("model", breed.model());
        }

        try {
            folder.mkdirs();
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la raza " + breed.id(), e);
        }
    }

}
