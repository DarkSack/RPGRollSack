package com.sack.rpgroll.ascension.deferred;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class TitleDefinitionWriter {

    private final File folder;

    public TitleDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "titles");
    }

    public void save(Title title) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", title.id());
        config.set("display-name", title.displayName());

        try {
            folder.mkdirs();
            config.save(new File(folder, title.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el título " + title.id(), e);
        }
    }

}
