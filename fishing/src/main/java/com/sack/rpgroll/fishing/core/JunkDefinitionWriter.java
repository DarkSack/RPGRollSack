package com.sack.rpgroll.fishing.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class JunkDefinitionWriter {

    private final File folder;

    public JunkDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "junk");
    }

    public void save(Junk junk) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", junk.id());
        config.set("display-name", junk.displayName());
        config.set("icon", junk.icon());
        config.set("description", junk.description());
        config.set("weight", junk.weight());

        try {
            folder.mkdirs();
            config.save(new File(folder, junk.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la basura " + junk.id(), e);
        }
    }

}
