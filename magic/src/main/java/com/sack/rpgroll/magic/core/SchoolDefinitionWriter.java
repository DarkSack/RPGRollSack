package com.sack.rpgroll.magic.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class SchoolDefinitionWriter {

    private final File folder;

    public SchoolDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "schools");
    }

    public void save(MagicSchool school) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", school.id());
        config.set("display-name", school.displayName());
        config.set("color", school.color());
        config.set("icon", school.icon());
        config.set("description", school.description());
        config.set("cast-sound", school.castSoundOnCast());
        config.set("cast-effect", school.castEffectId());

        for (var entry : school.raceAffinities().entrySet()) {
            config.set("race-affinities." + entry.getKey(), entry.getValue());
        }

        for (var entry : school.classAffinities().entrySet()) {
            config.set("class-affinities." + entry.getKey(), entry.getValue());
        }

        try {
            folder.mkdirs();
            config.save(new File(folder, school.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la escuela " + school.id(), e);
        }
    }

}
