package com.sack.rpgroll.crafting.grindstone;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class GrindstoneRecipeManager extends ContentManager<GrindstoneRecipeDefinition> {

    private final GrindstoneRecipeDefinitionWriter writer;

    public GrindstoneRecipeManager(JavaPlugin craftingPlugin) {
        super(owningPlugin(), new YamlLoader(craftingPlugin), "grindstone-recipes", "receta de amolar",
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

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(GrindstoneRecipeManager.class);
    }

}
