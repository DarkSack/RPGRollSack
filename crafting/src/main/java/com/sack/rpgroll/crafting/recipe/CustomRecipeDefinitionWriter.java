package com.sack.rpgroll.crafting.recipe;

import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomRecipeDefinitionWriter {

    private final File folder;

    public CustomRecipeDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "recipes");
    }

    public void save(CustomRecipe recipe) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", recipe.id());
        config.set("display-name", recipe.displayName());
        config.set("icon", recipe.icon());
        config.set("station-id", recipe.stationId());
        config.set("processing-time-ticks", recipe.processingTimeTicks());
        config.set("fuel-per-craft", recipe.fuelPerCraft());
        config.set("xp-amount", recipe.xpAmount());
        config.set("economy-currency-id", recipe.economyCurrencyId());
        config.set("economy-cost", recipe.economyCost());
        config.set("fail-chance", recipe.failChance());
        config.set("quality-enabled", recipe.qualityEnabled());

        config.createSection("result");
        config.set("result.type", recipe.result().type().name());
        config.set("result.value", recipe.result().value());
        config.set("result.amount", recipe.result().amount());

        config.set("ingredients", recipe.ingredients().stream().map(this::serializeIngredient).toList());
        config.set("conditions", recipe.conditions().stream().map(this::serializeCondition).toList());

        try {
            folder.mkdirs();
            config.save(new File(folder, recipe.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la receta " + recipe.id(), e);
        }
    }

    public void delete(String id) {
        new File(folder, id + ".yml").delete();
    }

    private Map<String, Object> serializeIngredient(IngredientSpec spec) {

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", spec.type().name());
        map.put("value", spec.value());
        map.put("amount", spec.amount());
        if (spec.hasMinQuality()) {
            map.put("min-quality", spec.minQuality());
        }
        return map;
    }

    private Map<String, Object> serializeCondition(RecipeCondition condition) {

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", condition.type().name());
        if (condition.value() != null) {
            map.put("value", condition.value());
        }
        map.put("min-value", condition.minValue());
        return map;
    }

}
