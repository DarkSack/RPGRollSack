package com.sack.rpgroll.crafting.recipe;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class CustomRecipeManager extends ContentManager<CustomRecipe> {

    private final CustomRecipeDefinitionWriter writer;

    public CustomRecipeManager(JavaPlugin craftingPlugin) {
        super(owningPlugin(), new YamlLoader(craftingPlugin), "recipes", "receta", new CustomRecipeParser());
        this.writer = new CustomRecipeDefinitionWriter(craftingPlugin.getDataFolder());
    }

    public void save(CustomRecipe recipe) {
        writer.save(recipe);
        reload();
    }

    public void delete(String id) {
        writer.delete(id);
        reload();
    }

    public List<CustomRecipe> byStation(String stationId) {
        return getAll().stream().filter(recipe -> recipe.stationId().equals(stationId)).toList();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(CustomRecipeManager.class);
    }

}
