package com.sack.rpgroll.crafting.vanilla;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class VanillaRecipeManager extends ContentManager<VanillaRecipeDefinition> {

    private final VanillaRecipeDefinitionWriter writer;

    public VanillaRecipeManager(JavaPlugin craftingPlugin) {
        super(owningPlugin(), new YamlLoader(craftingPlugin), "vanilla-recipes", "receta vanilla",
                new VanillaRecipeParser());
        this.writer = new VanillaRecipeDefinitionWriter(craftingPlugin.getDataFolder());
    }

    public void save(VanillaRecipeDefinition recipe) {
        writer.save(recipe);
        reload();
    }

    public void delete(String id) {
        writer.delete(id);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(VanillaRecipeManager.class);
    }

}
