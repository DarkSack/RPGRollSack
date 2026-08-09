package com.sack.rpgroll.crafting.brewing;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.crafting.condition.ConditionType;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.ingredient.IngredientType;
import com.sack.rpgroll.crafting.recipe.RecipeResult;
import com.sack.rpgroll.crafting.recipe.RecipeResultType;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BrewRecipeParser implements ContentParser<BrewRecipeDefinition> {

    @Override
    public BrewRecipeDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        ConfigurationSection ingredientSection = config.getConfigurationSection("ingredient");
        if (ingredientSection == null) {
            throw new IllegalArgumentException("receta de fermentación '" + id + "' sin sección obligatoria 'ingredient'");
        }

        IngredientType type;
        try {
            type = IngredientType.valueOf(ingredientSection.getString("type", "MATERIAL").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("receta de fermentación '" + id + "' tiene ingredient.type inválido");
        }

        IngredientSpec ingredient = new IngredientSpec(type, ingredientSection.getString("value"),
                ingredientSection.getInt("amount", 1), ingredientSection.getString("min-quality"));

        ConfigurationSection resultSection = config.getConfigurationSection("result");
        if (resultSection == null) {
            throw new IllegalArgumentException("receta de fermentación '" + id + "' sin sección obligatoria 'result'");
        }

        RecipeResultType resultType;
        try {
            resultType = RecipeResultType.valueOf(resultSection.getString("type", "MATERIAL").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("receta de fermentación '" + id + "' tiene result.type inválido");
        }

        RecipeResult result = new RecipeResult(resultType, resultSection.getString("value"), resultSection.getInt("amount", 1));

        return new BrewRecipeDefinition(id, ingredient, result, parseConditions(config.getMapList("conditions"), id));
    }

    private List<RecipeCondition> parseConditions(List<?> raw, String recipeId) {

        List<RecipeCondition> conditions = new ArrayList<>();

        for (Object entry : raw) {

            if (!(entry instanceof Map<?, ?> map) || map.get("type") == null) {
                continue;
            }

            ConditionType type;
            try {
                type = ConditionType.valueOf(map.get("type").toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "receta de fermentación '" + recipeId + "' tiene una condición con type inválido: " + map.get("type"));
            }

            String value = map.get("value") != null ? map.get("value").toString() : null;
            int minValue = map.get("min-value") != null ? Integer.parseInt(map.get("min-value").toString()) : 0;

            conditions.add(new RecipeCondition(type, value, minValue));
        }

        return conditions;
    }

}
