package com.sack.rpgroll.crafting.loom;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class LoomRecipeManager extends ContentManager<LoomRecipeDefinition> {

    private final LoomRecipeDefinitionWriter writer;

    public LoomRecipeManager(JavaPlugin craftingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(craftingPlugin), "loom-recipes", "receta de telar",
                new LoomRecipeParser());
        this.writer = new LoomRecipeDefinitionWriter(craftingPlugin.getDataFolder());
    }

    public void save(LoomRecipeDefinition recipe) {
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
