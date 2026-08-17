package com.sack.rpgroll.crafting.grindstone;

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

public class GrindstoneRecipeParser implements ContentParser<GrindstoneRecipeDefinition> {

    @Override
    public GrindstoneRecipeDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        IngredientSpec upper = parseIngredient(config.getConfigurationSection("upper"), id, "upper");
        IngredientSpec lower = parseIngredient(config.getConfigurationSection("lower"), id, "lower");
        RecipeResult result = parseResult(config.getConfigurationSection("result"), id);

        return new GrindstoneRecipeDefinition(id, upper, lower, result, parseConditions(config.getMapList("conditions"), id));
    }

    private IngredientSpec parseIngredient(ConfigurationSection section, String recipeId, String field) {

        if (section == null) {
            throw new IllegalArgumentException("receta de amolar '" + recipeId + "' sin sección obligatoria '" + field + "'");
        }

        IngredientType type;
        try {
            type = IngredientType.valueOf(section.getString("type", "MATERIAL").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("receta de amolar '" + recipeId + "' tiene " + field + ".type inválido");
        }

        return new IngredientSpec(type, section.getString("value"), section.getInt("amount", 1),
                section.getString("min-quality"));
    }

    private RecipeResult parseResult(ConfigurationSection section, String recipeId) {

        if (section == null) {
            throw new IllegalArgumentException("receta de amolar '" + recipeId + "' sin sección obligatoria 'result'");
        }

        RecipeResultType type;
        try {
            type = RecipeResultType.valueOf(section.getString("type", "MATERIAL").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("receta de amolar '" + recipeId + "' tiene result.type inválido");
        }

        return new RecipeResult(type, section.getString("value"), section.getInt("amount", 1));
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
                        "receta de amolar '" + recipeId + "' tiene una condición con type inválido: " + map.get("type"));
            }

            String value = map.get("value") != null ? map.get("value").toString() : null;
            int minValue = map.get("min-value") != null ? Integer.parseInt(map.get("min-value").toString()) : 0;

            conditions.add(new RecipeCondition(type, value, minValue));
        }

        return conditions;
    }

}
