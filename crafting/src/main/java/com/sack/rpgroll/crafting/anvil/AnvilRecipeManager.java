package com.sack.rpgroll.crafting.anvil;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class AnvilRecipeManager extends ContentManager<AnvilRecipeDefinition> {

    private final AnvilRecipeDefinitionWriter writer;

    public AnvilRecipeManager(JavaPlugin craftingPlugin) {
        super(owningPlugin(), new YamlLoader(craftingPlugin), "anvil-recipes", "receta de yunque",
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

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(AnvilRecipeManager.class);
    }

}
