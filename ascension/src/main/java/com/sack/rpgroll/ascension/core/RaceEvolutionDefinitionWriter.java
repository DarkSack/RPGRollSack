package com.sack.rpgroll.ascension.core;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class RaceEvolutionDefinitionWriter {

    private final File folder;

    public RaceEvolutionDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "evolutions");
    }

    public void save(RaceEvolution evolution) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", evolution.id());
        config.set("base-race", evolution.baseRace());
        config.set("display-name", evolution.displayName());
        AscensionRequirementsWriter.write(config, "requirements", evolution.requirements());

        ConfigurationSection stats = config.createSection("stats");
        evolution.statBonus().forEach(stats::set);

        config.set("traits", evolution.grantedTraits());
        config.set("skills", evolution.grantedSkills());

        ConfigurationSection affinities = config.createSection("affinities");
        evolution.affinityBonus().forEach(affinities::set);

        ConfigurationSection resistances = config.createSection("resistances");
        evolution.resistances().forEach(resistances::set);

        ConfigurationSection weaknesses = config.createSection("weaknesses");
        evolution.weaknesses().forEach(weaknesses::set);

        config.set("professions", evolution.unlockedProfessions());

        try {
            folder.mkdirs();
            config.save(new File(folder, evolution.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la evolución " + evolution.id(), e);
        }
    }

}
