package com.sack.rpgroll.fishing.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FishSpeciesDefinitionWriter {

    private final File folder;

    public FishSpeciesDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "species");
    }

    public void save(FishSpecies species) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", species.id());
        config.set("display-name", species.displayName());
        config.set("icon", species.icon());
        config.set("custom-model-data", species.customModelData());
        config.set("description", species.description());
        config.set("category", species.category().name());
        config.set("rarity", species.rarity().name());
        config.set("water-types", species.waterTypes().stream().map(Enum::name).toList());
        config.set("biomes", List.copyOf(species.biomes()));
        config.set("depths", species.depths().stream().map(Enum::name).toList());
        config.set("min-weight", species.minWeight());
        config.set("max-weight", species.maxWeight());
        config.set("min-length", species.minLength());
        config.set("max-length", species.maxLength());
        config.set("base-price", species.basePrice());
        config.set("base-experience", species.baseExperience());
        config.set("behavior", species.behavior().name());
        config.set("allowed-seasons", List.copyOf(species.allowedSeasons()));
        config.set("allowed-weathers", species.allowedWeathers().stream().map(Enum::name).toList());
        config.set("allowed-times", species.allowedTimes().stream().map(Enum::name).toList());
        config.set("attracted-by-bait-tags", List.copyOf(species.attractedByBaitTags()));
        config.set("legendary", species.legendary());
        config.set("required-level", species.requiredLevel());
        config.set("requires-full-moon", species.requiresFullMoon());
        config.set("required-bait", species.requiredBaitId());
        config.set("catch-effect", species.catchEffectId());
        config.set("catch-status-effect", species.catchStatusEffectId());

        try {
            folder.mkdirs();
            config.save(new File(folder, species.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la especie " + species.id(), e);
        }
    }

}
