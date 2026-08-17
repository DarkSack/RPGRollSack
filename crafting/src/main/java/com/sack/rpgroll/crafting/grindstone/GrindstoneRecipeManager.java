package com.sack.rpgroll.crafting.grindstone;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class GrindstoneRecipeManager extends ContentManager<GrindstoneRecipeDefinition> {

    private final GrindstoneRecipeDefinitionWriter writer;

    public GrindstoneRecipeManager(JavaPlugin craftingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(craftingPlugin), "grindstone-recipes", "receta de amolar",
                new GrindstoneRecipeParser());
        this.writer = new GrindstoneRecipeDefinitionWriter(craftingPlugin.getDataFolder());
    }

    public void save(GrindstoneRecipeDefinition recipe) {
        writer.save(recipe);
        reload();
    }

    public void delete(String id) {
        writer.delete(id);
        reload();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
