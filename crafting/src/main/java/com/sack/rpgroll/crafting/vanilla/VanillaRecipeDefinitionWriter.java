package com.sack.rpgroll.crafting.vanilla;

import com.sack.rpgroll.crafting.condition.RecipeCondition;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class VanillaRecipeDefinitionWriter {

    private final File folder;

    public VanillaRecipeDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "vanilla-recipes");
    }

    public void save(VanillaRecipeDefinition recipe) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", recipe.id());
        config.set("type", recipe.type().name());
        config.set("shape", recipe.shape());
        config.set("ingredients", recipe.ingredients());
        config.set("template-material", recipe.templateMaterial());
        config.set("base-material", recipe.baseMaterial());
        config.set("cooking-time-ticks", recipe.cookingTimeTicks());
        config.set("experience", recipe.experience());

        for (var entry : recipe.key().entrySet()) {
            config.set("key." + entry.getKey(), entry.getValue());
        }

        config.set("result.type", recipe.result().type().name());
        config.set("result.value", recipe.result().value());
        config.set("result.amount", recipe.result().amount());

        config.set("conditions", recipe.conditions().stream().map(this::serializeCondition).toList());

        try {
            folder.mkdirs();
            config.save(new File(folder, recipe.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la receta vanilla " + recipe.id(), e);
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
