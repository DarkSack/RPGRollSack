package com.sack.rpgroll.crafting.cartography;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class CartographyRecipeManager extends ContentManager<CartographyRecipeDefinition> {

    private final CartographyRecipeDefinitionWriter writer;

    public CartographyRecipeManager(JavaPlugin craftingPlugin) {
        super(owningPlugin(), new YamlLoader(craftingPlugin), "cartography-recipes", "receta de cartografía",
                new CartographyRecipeParser());
        this.writer = new CartographyRecipeDefinitionWriter(craftingPlugin.getDataFolder());
    }

    public void save(CartographyRecipeDefinition recipe) {
        writer.save(recipe);
        reload();
    }

    public void delete(String id) {
        writer.delete(id);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(CartographyRecipeManager.class);
    }

}
