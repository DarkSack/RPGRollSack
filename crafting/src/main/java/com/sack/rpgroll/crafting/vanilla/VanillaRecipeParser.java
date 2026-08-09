package com.sack.rpgroll.crafting.vanilla;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.crafting.condition.ConditionType;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.recipe.RecipeResult;
import com.sack.rpgroll.crafting.recipe.RecipeResultType;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VanillaRecipeParser implements ContentParser<VanillaRecipeDefinition> {

    @Override
    public VanillaRecipeDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String rawType = config.getString("type");
        VanillaStationType type;
        try {
            type = VanillaStationType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("receta vanilla '" + id + "' tiene type inválido: " + rawType);
        }

        RecipeResult result = parseResult(config, id);

        Map<String, String> key = new LinkedHashMap<>();
        ConfigurationSection keySection = config.getConfigurationSection("key");
        if (keySection != null) {
            for (String letter : keySection.getKeys(false)) {
                key.put(letter, keySection.getString(letter));
            }
        }

        return new VanillaRecipeDefinition(
                id,
                type,
                config.getStringList("shape"),
                key,
                config.getStringList("ingredients"),
                config.getString("template-material"),
                config.getString("base-material"),
                config.getInt("cooking-time-ticks", 200),
                (float) config.getDouble("experience", 0),
                result,
                parseConditions(config.getMapList("conditions"), id));
    }

    private RecipeResult parseResult(YamlConfiguration config, String recipeId) {

        var section = config.getConfigurationSection("result");
        if (section == null) {
            throw new IllegalArgumentException("receta vanilla '" + recipeId + "' sin sección obligatoria 'result'");
        }

        RecipeResultType type;
        try {
            type = RecipeResultType.valueOf(section.getString("type", "MATERIAL").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("receta vanilla '" + recipeId + "' tiene result.type inválido");
        }

        String value = section.getString("value");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("receta vanilla '" + recipeId + "' sin result.value");
        }

        return new RecipeResult(type, value, section.getInt("amount", 1));
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
                        "receta vanilla '" + recipeId + "' tiene una condición con type inválido: " + map.get("type"));
            }

            String value = map.get("value") != null ? map.get("value").toString() : null;
            int minValue = map.get("min-value") != null ? Integer.parseInt(map.get("min-value").toString()) : 0;

            conditions.add(new RecipeCondition(type, value, minValue));
        }

        return conditions;
    }

}
