package com.sack.rpgroll.crafting.recipe;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.crafting.condition.ConditionType;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.ingredient.IngredientType;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomRecipeParser implements ContentParser<CustomRecipe> {

    @Override
    public CustomRecipe parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String stationId = config.getString("station-id");
        if (stationId == null || stationId.isBlank()) {
            throw new IllegalArgumentException("receta '" + id + "' sin campo obligatorio 'station-id'");
        }

        RecipeResult result = parseResult(config, id);
        List<IngredientSpec> ingredients = parseIngredients(config.getMapList("ingredients"), id);
        List<RecipeCondition> conditions = parseConditions(config.getMapList("conditions"), id);

        return new CustomRecipe(
                id,
                config.getString("display-name", id),
                config.getString("icon", "CRAFTING_TABLE"),
                stationId,
                ingredients,
                result,
                conditions,
                config.getInt("processing-time-ticks", 100),
                config.getInt("fuel-per-craft", 0),
                config.getDouble("xp-amount", 0),
                config.getString("economy-currency-id"),
                config.getDouble("economy-cost", 0),
                config.getDouble("fail-chance", -1),
                config.getBoolean("quality-enabled", false));
    }

    private RecipeResult parseResult(YamlConfiguration config, String recipeId) {

        var section = config.getConfigurationSection("result");
        if (section == null) {
            throw new IllegalArgumentException("receta '" + recipeId + "' sin sección obligatoria 'result'");
        }

        String rawType = section.getString("type", "MATERIAL");
        RecipeResultType type;

        try {
            type = RecipeResultType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("receta '" + recipeId + "' tiene result.type inválido: " + rawType);
        }

        String value = section.getString("value");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("receta '" + recipeId + "' sin result.value");
        }

        return new RecipeResult(type, value, section.getInt("amount", 1));
    }

    private List<IngredientSpec> parseIngredients(List<?> raw, String recipeId) {

        List<IngredientSpec> ingredients = new ArrayList<>();

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }

            Object rawType = map.get("type");
            if (rawType == null) {
                continue;
            }

            IngredientType type;
            try {
                type = IngredientType.valueOf(rawType.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "receta '" + recipeId + "' tiene un ingrediente con type inválido: " + rawType);
            }

            String value = map.get("value") != null ? map.get("value").toString() : null;
            int amount = map.get("amount") != null ? Integer.parseInt(map.get("amount").toString()) : 1;
            String minQuality = map.get("min-quality") != null ? map.get("min-quality").toString() : null;

            ingredients.add(new IngredientSpec(type, value, amount, minQuality));
        }

        return ingredients;
    }

    private List<RecipeCondition> parseConditions(List<?> raw, String recipeId) {

        List<RecipeCondition> conditions = new ArrayList<>();

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }

            Object rawType = map.get("type");
            if (rawType == null) {
                continue;
            }

            ConditionType type;
            try {
                type = ConditionType.valueOf(rawType.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "receta '" + recipeId + "' tiene una condición con type inválido: " + rawType);
            }

            String value = map.get("value") != null ? map.get("value").toString() : null;
            int minValue = map.get("min-value") != null ? Integer.parseInt(map.get("min-value").toString()) : 0;

            conditions.add(new RecipeCondition(type, value, minValue));
        }

        return conditions;
    }

}
