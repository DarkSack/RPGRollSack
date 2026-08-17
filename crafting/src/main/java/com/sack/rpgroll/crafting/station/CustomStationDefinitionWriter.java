package com.sack.rpgroll.crafting.station;

import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.station.structure.StructureRequirement;
import com.sack.rpgroll.crafting.station.tier.TierUpgrade;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        config.set("structure-requirements", station.structureRequirements().stream().map(this::serializeStructure).toList());
        config.set("max-tier", station.maxTier());
        config.set("tier-upgrades", station.tierUpgrades().stream().map(this::serializeTierUpgrade).toList());
        config.set("speed-bonus-per-tier", station.speedBonusPerTier());
        config.set("fail-reduction-per-tier", station.failReductionPerTier());
        config.set("skill-category", station.skillCategory());
        config.set("allow-experimentation", station.allowExperimentation());

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

    private Map<String, Object> serializeStructure(StructureRequirement requirement) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("dx", requirement.dx());
        map.put("dy", requirement.dy());
        map.put("dz", requirement.dz());
        map.put("material", requirement.material());
        return map;
    }

    private Map<String, Object> serializeTierUpgrade(TierUpgrade upgrade) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("tier", upgrade.tier());
        map.put("economy-cost", upgrade.economyCost());
        if (upgrade.economyCurrencyId() != null) {
            map.put("economy-currency-id", upgrade.economyCurrencyId());
        }
        map.put("cost", upgrade.cost().stream().map(this::serializeIngredient).toList());
        return map;
    }

    private Map<String, Object> serializeIngredient(IngredientSpec spec) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", spec.type().name());
        map.put("value", spec.value());
        map.put("amount", spec.amount());
        return map;
    }

}
