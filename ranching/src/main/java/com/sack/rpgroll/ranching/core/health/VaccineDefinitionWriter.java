package com.sack.rpgroll.ranching.core.health;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class VaccineDefinitionWriter {

    private final File folder;

    public VaccineDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "vaccines");
    }

    public void save(Vaccine vaccine) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", vaccine.id());
        config.set("display-name", vaccine.displayName());
        config.set("icon", vaccine.icon());
        config.set("description", vaccine.description());
        config.set("prevents-diseases", List.copyOf(vaccine.preventsDiseaseIds()));
        config.set("risk-reduction", vaccine.riskReduction());
        config.set("immunity-duration-ticks", vaccine.immunityDurationTicks());

        try {
            folder.mkdirs();
            config.save(new File(folder, vaccine.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la vacuna " + vaccine.id(), e);
        }
    }

}
