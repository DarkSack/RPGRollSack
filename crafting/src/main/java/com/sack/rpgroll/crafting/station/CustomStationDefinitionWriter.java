package com.sack.rpgroll.crafting.station;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CustomStationDefinitionWriter {

    private final File folder;

    public CustomStationDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "stations");
    }

    public void save(CustomStation station) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", station.id());
        config.set("display-name", station.displayName());
        config.set("icon", station.icon());
        config.set("trigger-block-material", station.triggerBlockMaterial());
        config.set("inventory-size", station.inventorySize());
        config.set("ingredient-slots", List.copyOf(station.ingredientSlots()));
        config.set("fuel-slot", station.fuelSlot());
        config.set("output-slot", station.outputSlot());
        config.set("requires-fuel", station.requiresFuel());
        config.set("gui-title", station.guiTitle());
        config.set("allowed-recipe-ids", List.copyOf(station.allowedRecipeIds()));

        try {
            folder.mkdirs();
            config.save(new File(folder, station.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la estación " + station.id(), e);
        }
    }

    public void delete(String id) {
        new File(folder, id + ".yml").delete();
    }

}
