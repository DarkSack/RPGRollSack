package com.sack.rpgroll.ranching.core.health;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MedicineDefinitionWriter {

    private final File folder;

    public MedicineDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "medicines");
    }

    public void save(Medicine medicine) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", medicine.id());
        config.set("display-name", medicine.displayName());
        config.set("icon", medicine.icon());
        config.set("description", medicine.description());
        config.set("type", medicine.type().name());
        config.set("cures-diseases", List.copyOf(medicine.curesDiseaseIds()));
        config.set("cure-chance", medicine.cureChance());
        config.set("recovery-boost-ticks", medicine.recoveryBoostTicks());
        config.set("health-bonus", medicine.healthBonus());
        config.set("happiness-bonus", medicine.happinessBonus());

        try {
            folder.mkdirs();
            config.save(new File(folder, medicine.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la medicina " + medicine.id(), e);
        }
    }

}
