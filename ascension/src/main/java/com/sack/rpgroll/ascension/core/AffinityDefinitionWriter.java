package com.sack.rpgroll.ascension.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class AffinityDefinitionWriter {

    private final File folder;

    public AffinityDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "affinities");
    }

    public void save(Affinity affinity) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", affinity.id());
        config.set("display-name", affinity.displayName());

        if (affinity.opposingId() != null) {
            config.set("opposing", affinity.opposingId());
        }

        config.set("resist-causes", affinity.resistCauses());

        try {
            folder.mkdirs();
            config.save(new File(folder, affinity.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la afinidad " + affinity.id(), e);
        }
    }

}
