package com.sack.rpgroll.fishing.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FishingRodDefinitionWriter {

    private final File folder;

    public FishingRodDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "rods");
    }

    public void save(FishingRod rod) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", rod.id());
        config.set("display-name", rod.displayName());
        config.set("material", rod.material());
        config.set("description", rod.description());
        config.set("durability", rod.durability());
        config.set("cast-power", rod.castPower());
        config.set("reel-speed", rod.reelSpeed());
        config.set("precision", rod.precision());
        config.set("resistance", rod.resistance());
        config.set("luck-bonus", rod.luckBonus());
        config.set("preferred-categories", List.copyOf(rod.preferredCategories()));

        try {
            folder.mkdirs();
            config.save(new File(folder, rod.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la caña " + rod.id(), e);
        }
    }

}
