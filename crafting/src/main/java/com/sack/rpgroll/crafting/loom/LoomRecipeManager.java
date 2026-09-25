package com.sack.rpgroll.crafting.loom;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class LoomRecipeManager extends ContentManager<LoomRecipeDefinition> {

    private final LoomRecipeDefinitionWriter writer;

    public LoomRecipeManager(JavaPlugin craftingPlugin) {
        super(owningPlugin(), new YamlLoader(craftingPlugin), "loom-recipes", "receta de telar",
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

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(LoomRecipeManager.class);
    }

}
