package com.sack.rpgroll.magic.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GrimoireDefinitionWriter {

    private final File folder;

    public GrimoireDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "grimoires");
    }

    public void save(Grimoire grimoire) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", grimoire.id());
        config.set("display-name", grimoire.displayName());
        config.set("icon", grimoire.icon());
        config.set("description", grimoire.description());
        config.set("school", grimoire.schoolId());
        config.set("required-level", grimoire.requiredLevel());
        config.set("spells", List.copyOf(grimoire.spellIds()));

        try {
            folder.mkdirs();
            config.save(new File(folder, grimoire.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el grimorio " + grimoire.id(), e);
        }
    }

}
