package com.sack.rpgroll.crafting.fuel;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FuelDefinitionWriter {

    private final File folder;

    public FuelDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "fuels");
    }

    public void save(FuelDefinition fuel) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", fuel.id());
        config.set("display-name", fuel.displayName());
        config.set("icon", fuel.icon());
        config.set("material-or-item-id", fuel.materialOrItemId());
        config.set("is-custom-item", fuel.isCustomItem());
        config.set("burn-ticks", fuel.burnTicks());
        config.set("consume-amount", fuel.consumeAmount());

        try {
            folder.mkdirs();
            config.save(new File(folder, fuel.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el combustible " + fuel.id(), e);
        }
    }

    public void delete(String id) {
        new File(folder, id + ".yml").delete();
    }

}
