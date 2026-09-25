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

        // Se escribe encima del fichero que ya hay, no se rehace: el editor
        // solo conoce estos campos, y criterios, rangos o recompensas se
        // escriben a mano en el YAML. Rehacerlo los borraba al renombrar.
        File file = new File(folder, faction.id() + ".yml");
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        config.set("id", faction.id());
        config.set("display-name", faction.displayName());

        try {
            folder.mkdirs();
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la facción " + faction.id(), e);
        }
    }

}
