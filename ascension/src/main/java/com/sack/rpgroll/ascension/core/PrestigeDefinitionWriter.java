package com.sack.rpgroll.ascension.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class PrestigeDefinitionWriter {

    private final File folder;

    public PrestigeDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "prestige");
    }

    public void save(PrestigeLevel level) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", level.id());
        config.set("required-level", level.requiredLevel());
        config.set("exp-bonus-percent", level.expBonusPercent());
        config.set("skills", level.grantedSkills());

        try {
            folder.mkdirs();
            config.save(new File(folder, level.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el prestigio " + level.id(), e);
        }
    }

}
