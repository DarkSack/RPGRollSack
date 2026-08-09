package com.sack.rpgroll.crafting.station;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashSet;
import java.util.List;
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
                allowedRecipeIds);
    }

}
