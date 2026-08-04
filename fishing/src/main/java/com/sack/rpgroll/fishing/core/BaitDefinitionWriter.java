package com.sack.rpgroll.fishing.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class BaitDefinitionWriter {

    private final File folder;

    public BaitDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "baits");
    }

    public void save(Bait bait) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", bait.id());
        config.set("display-name", bait.displayName());
        config.set("material", bait.material());
        config.set("description", bait.description());
        config.set("tags", List.copyOf(bait.tags()));
        config.set("quality-bonus", bait.qualityBonus());
        config.set("legendary-weight-multiplier", bait.legendaryWeightMultiplier());

        try {
            folder.mkdirs();
            config.save(new File(folder, bait.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la carnada " + bait.id(), e);
        }
    }

}
