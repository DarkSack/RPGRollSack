package com.sack.rpgroll.ascension.deferred;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FactionDefinitionWriter {

    private final File folder;

    public FactionDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "factions");
    }

    public void save(Faction faction) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", faction.id());
        config.set("display-name", faction.displayName());

        try {
            folder.mkdirs();
            config.save(new File(folder, faction.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la facción " + faction.id(), e);
        }
    }

}
