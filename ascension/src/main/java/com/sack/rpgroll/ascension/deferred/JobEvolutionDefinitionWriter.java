package com.sack.rpgroll.ascension.deferred;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class JobEvolutionDefinitionWriter {

    private final File folder;

    public JobEvolutionDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "job-evolutions");
    }

    public void save(JobEvolution evolution) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", evolution.id());
        config.set("base-job", evolution.baseJob());
        config.set("display-name", evolution.displayName());
        config.set("required-job-level", evolution.requiredJobLevel());
        config.set("unlocked-recipes", evolution.unlockedRecipes());
        config.set("unlocked-tools", evolution.unlockedTools());
        config.set("unlocked-quests", evolution.unlockedQuests());

        try {
            folder.mkdirs();
            config.save(new File(folder, evolution.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la evolución de job " + evolution.id(), e);
        }
    }

}
