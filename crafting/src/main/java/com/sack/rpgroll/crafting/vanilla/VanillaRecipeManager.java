package com.sack.rpgroll.crafting.vanilla;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class VanillaRecipeManager extends ContentManager<VanillaRecipeDefinition> {

    private final VanillaRecipeDefinitionWriter writer;

    public VanillaRecipeManager(JavaPlugin craftingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(craftingPlugin), "vanilla-recipes", "receta vanilla",
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

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
