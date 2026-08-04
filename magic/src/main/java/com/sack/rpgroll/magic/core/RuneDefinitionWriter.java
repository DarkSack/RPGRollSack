package com.sack.rpgroll.magic.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class RuneDefinitionWriter {

    private final File folder;

    public RuneDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "runes");
    }

    public void save(Rune rune) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", rune.id());
        config.set("display-name", rune.displayName());
        config.set("icon", rune.icon());
        config.set("description", rune.description());
        config.set("type", rune.type().name());

        for (var entry : rune.params().entrySet()) {
            config.set("params." + entry.getKey(), entry.getValue());
        }

        try {
            folder.mkdirs();
            config.save(new File(folder, rune.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la runa " + rune.id(), e);
        }
    }

}
