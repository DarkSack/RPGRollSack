package com.sack.rpgroll.crafting.anvil;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class AnvilRecipeManager extends ContentManager<AnvilRecipeDefinition> {

    private final AnvilRecipeDefinitionWriter writer;

    public AnvilRecipeManager(JavaPlugin craftingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(craftingPlugin), "anvil-recipes", "receta de yunque",
                new AnvilRecipeParser());
        this.writer = new AnvilRecipeDefinitionWriter(craftingPlugin.getDataFolder());
    }

    public void save(AnvilRecipeDefinition recipe) {
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
