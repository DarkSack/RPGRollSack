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

        // Se escribe encima del fichero que ya hay, no se rehace: el editor
        // solo conoce estos campos, y criterios, rangos o recompensas se
        // escriben a mano en el YAML. Rehacerlo los borraba al renombrar.
        File file = new File(folder, title.id() + ".yml");
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        config.set("id", title.id());
        config.set("display-name", title.displayName());

        try {
            folder.mkdirs();
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el título " + title.id(), e);
        }
    }

}
