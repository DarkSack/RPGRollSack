package com.sack.rpgroll.ranching.core.species;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SpeciesDefinitionWriter {

    private final File folder;

    public SpeciesDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "species");
    }

    public void save(Species species) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", species.id());
        config.set("display-name", species.displayName());
        config.set("icon", species.icon());
        config.set("description", species.description());
        config.set("entity-type", species.entityType());
        config.set("product-types", List.copyOf(species.productTypes()));

        for (var entry : species.baseProduction().entrySet()) {
            config.set("base-production." + entry.getKey(), entry.getValue());
        }

        config.set("base-weight-min", species.baseWeightMin());
        config.set("base-weight-max", species.baseWeightMax());
        config.set("baby-stage-duration-ticks", species.babyStageDurationTicks());
        config.set("juvenile-stage-duration-ticks", species.juvenileStageDurationTicks());
        config.set("elder-threshold-ticks", species.elderThresholdTicks());
        config.set("gestation-duration-ticks", species.gestationDurationTicks());
        config.set("min-litter-size", species.minLitterSize());
        config.set("max-litter-size", species.maxLitterSize());
        config.set("base-fertility", species.baseFertility());
        config.set("diet-tags", List.copyOf(species.dietTags()));

        try {
            folder.mkdirs();
            config.save(new File(folder, species.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la especie " + species.id(), e);
        }
    }

}
