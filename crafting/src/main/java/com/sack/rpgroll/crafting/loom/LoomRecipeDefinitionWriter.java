package com.sack.rpgroll.crafting.loom;

import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoomRecipeDefinitionWriter {

    private final File folder;

    public LoomRecipeDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "loom-recipes");
    }

    public void save(LoomRecipeDefinition recipe) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", recipe.id());

        writeIngredient(config, "banner", recipe.bannerIngredient());
        writeIngredient(config, "dye", recipe.dyeIngredient());
        if (recipe.hasPatternIngredient()) {
            writeIngredient(config, "pattern", recipe.patternIngredient());
        }

        config.set("result.type", recipe.result().type().name());
        config.set("result.value", recipe.result().value());
        config.set("result.amount", recipe.result().amount());

        config.set("conditions", recipe.conditions().stream().map(this::serializeCondition).toList());

        try {
            folder.mkdirs();
            config.save(new File(folder, recipe.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la receta de telar " + recipe.id(), e);
        }
    }

    public void delete(String id) {
        new File(folder, id + ".yml").delete();
    }

    private void writeIngredient(YamlConfiguration config, String section, IngredientSpec spec) {
        config.set(section + ".type", spec.type().name());
        config.set(section + ".value", spec.value());
        config.set(section + ".amount", spec.amount());
        if (spec.hasMinQuality()) {
            config.set(section + ".min-quality", spec.minQuality());
        }
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
