package com.sack.rpgroll.crafting.recipe;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class CustomRecipeManager extends ContentManager<CustomRecipe> {

    private final CustomRecipeDefinitionWriter writer;

    public CustomRecipeManager(JavaPlugin craftingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(craftingPlugin), "recipes", "receta", new CustomRecipeParser());
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

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
