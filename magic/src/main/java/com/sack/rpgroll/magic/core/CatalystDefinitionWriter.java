package com.sack.rpgroll.magic.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class CatalystDefinitionWriter {

    private final File folder;

    public CatalystDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "catalysts");
    }

    public void save(SpellCatalyst catalyst) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", catalyst.id());
        config.set("display-name", catalyst.displayName());
        config.set("material", catalyst.material());
        config.set("description", catalyst.description());
        config.set("power-multiplier", catalyst.powerMultiplier());
        config.set("cost-multiplier", catalyst.costMultiplier());
        config.set("range-multiplier", catalyst.rangeMultiplier());

        try {
            folder.mkdirs();
            config.save(new File(folder, catalyst.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el catalizador " + catalyst.id(), e);
        }
    }

}
