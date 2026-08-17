package com.sack.rpgroll.crafting.cartography;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CartographyRecipeManager extends ContentManager<CartographyRecipeDefinition> {

    private final CartographyRecipeDefinitionWriter writer;

    public CartographyRecipeManager(JavaPlugin craftingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(craftingPlugin), "cartography-recipes", "receta de cartografía",
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

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
