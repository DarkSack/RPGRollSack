package com.sack.rpgroll.crafting.brewing;

import com.sack.rpgroll.crafting.condition.RecipeCondition;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class BrewRecipeDefinitionWriter {

    private final File folder;

    public BrewRecipeDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "brew-recipes");
    }

    public void save(BrewRecipeDefinition recipe) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", recipe.id());

        config.set("ingredient.type", recipe.ingredient().type().name());
        config.set("ingredient.value", recipe.ingredient().value());
        config.set("ingredient.amount", recipe.ingredient().amount());
        if (recipe.ingredient().hasMinQuality()) {
            config.set("ingredient.min-quality", recipe.ingredient().minQuality());
        }

        config.set("result.type", recipe.result().type().name());
        config.set("result.value", recipe.result().value());
        config.set("result.amount", recipe.result().amount());

        config.set("conditions", recipe.conditions().stream().map(this::serializeCondition).toList());

        try {
            folder.mkdirs();
            config.save(new File(folder, recipe.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la receta de fermentación " + recipe.id(), e);
        }
    }

    public void delete(String id) {
        new File(folder, id + ".yml").delete();
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
