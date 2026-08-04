package com.sack.rpgroll.ranching.core.health;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DiseaseDefinitionWriter {

    private final File folder;

    public DiseaseDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "diseases");
    }

    public void save(Disease disease) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", disease.id());
        config.set("display-name", disease.displayName());
        config.set("description", disease.description());
        config.set("symptoms", List.copyOf(disease.symptoms()));
        config.set("duration-ticks", disease.durationTicks());
        config.set("contagion-chance", disease.contagionChance());
        config.set("health-penalty-per-check", disease.healthPenaltyPerCheck());
        config.set("happiness-penalty-per-check", disease.happinessPenaltyPerCheck());
        config.set("production-penalty", disease.productionPenalty());

        try {
            folder.mkdirs();
            config.save(new File(folder, disease.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la enfermedad " + disease.id(), e);
        }
    }

}
