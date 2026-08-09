package com.sack.rpgroll.crafting.brewing;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BrewRecipeManager extends ContentManager<BrewRecipeDefinition> {

    private final BrewRecipeDefinitionWriter writer;

    public BrewRecipeManager(JavaPlugin craftingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(craftingPlugin), "brew-recipes", "receta de fermentación",
                new BrewRecipeParser());
        this.writer = new BrewRecipeDefinitionWriter(craftingPlugin.getDataFolder());
    }

    public void save(BrewRecipeDefinition recipe) {
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
