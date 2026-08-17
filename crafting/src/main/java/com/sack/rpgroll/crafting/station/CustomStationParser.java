package com.sack.rpgroll.crafting.station;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.ingredient.IngredientType;
import com.sack.rpgroll.crafting.station.structure.StructureRequirement;
import com.sack.rpgroll.crafting.station.tier.TierUpgrade;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CustomStationParser implements ContentParser<CustomStation> {

    @Override
    public CustomStation parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        List<Integer> ingredientSlots = config.getIntegerList("ingredient-slots");
        Set<String> allowedRecipeIds = new LinkedHashSet<>(config.getStringList("allowed-recipe-ids"));
        List<StructureRequirement> structureRequirements = parseStructure(config.getMapList("structure-requirements"));
        List<TierUpgrade> tierUpgrades = parseTierUpgrades(config.getMapList("tier-upgrades"), id);

        return new CustomStation(
                id,
                config.getString("display-name", id),
                config.getString("icon", "SMITHING_TABLE"),
                config.getString("trigger-block-material", "SMITHING_TABLE"),
                config.getInt("inventory-size", 27),
                ingredientSlots,
                config.getInt("fuel-slot", -1),
                config.getInt("output-slot", 8),
                config.getBoolean("requires-fuel", false),
                config.getString("gui-title"),
                allowedRecipeIds,
                structureRequirements,
                config.getInt("max-tier", 1),
                tierUpgrades,
                config.getDouble("speed-bonus-per-tier", 0),
                config.getDouble("fail-reduction-per-tier", 0),
                config.getString("skill-category"),
                config.getBoolean("allow-experimentation", false));
    }

    private List<StructureRequirement> parseStructure(List<?> raw) {

        List<StructureRequirement> requirements = new ArrayList<>();

        for (Object entry : raw) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }

            Object material = map.get("material");
            if (material == null) {
                continue;
            }

            int dx = map.get("dx") != null ? Integer.parseInt(map.get("dx").toString()) : 0;
            int dy = map.get("dy") != null ? Integer.parseInt(map.get("dy").toString()) : 0;
            int dz = map.get("dz") != null ? Integer.parseInt(map.get("dz").toString()) : 0;

            requirements.add(new StructureRequirement(dx, dy, dz, material.toString()));
        }

        return requirements;
    }

    private List<TierUpgrade> parseTierUpgrades(List<?> raw, String stationId) {

        List<TierUpgrade> upgrades = new ArrayList<>();

        for (Object entry : raw) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }

            int tier = map.get("tier") != null ? Integer.parseInt(map.get("tier").toString()) : 2;
            double economyCost = map.get("economy-cost") != null ? Double.parseDouble(map.get("economy-cost").toString()) : 0;
            String economyCurrencyId = map.get("economy-currency-id") != null ? map.get("economy-currency-id").toString() : null;

            List<IngredientSpec> cost = new ArrayList<>();
            Object rawCost = map.get("cost");
            if (rawCost instanceof List<?> costList) {
                for (Object costEntry : costList) {
                    if (!(costEntry instanceof Map<?, ?> costMap)) {
                        continue;
                    }
                    Object rawType = costMap.get("type");
                    IngredientType type = IngredientType.MATERIAL;
                    if (rawType != null) {
                        try {
                            type = IngredientType.valueOf(rawType.toString().trim().toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("estación '" + stationId
                                    + "' tiene un costo de tier con type inválido: " + rawType);
                        }
                    }
                    String value = costMap.get("value") != null ? costMap.get("value").toString() : null;
                    int amount = costMap.get("amount") != null ? Integer.parseInt(costMap.get("amount").toString()) : 1;
                    cost.add(new IngredientSpec(type, value, amount, null));
                }
            }

            upgrades.add(new TierUpgrade(tier, cost, economyCost, economyCurrencyId));
        }

        return upgrades;
    }

}
