package com.sack.rpgroll.crafting.brewing;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class BrewRecipeManager extends ContentManager<BrewRecipeDefinition> {

    private final BrewRecipeDefinitionWriter writer;

    public BrewRecipeManager(JavaPlugin craftingPlugin) {
        super(owningPlugin(), new YamlLoader(craftingPlugin), "brew-recipes", "receta de fermentación",
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

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(BrewRecipeManager.class);
    }

}
