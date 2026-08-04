package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class FishSpeciesManager extends ContentManager<FishSpecies> {

    private final FishSpeciesDefinitionWriter writer;

    public FishSpeciesManager(JavaPlugin fishingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(fishingPlugin), "species", "especie", new FishSpeciesParser());
        this.writer = new FishSpeciesDefinitionWriter(fishingPlugin.getDataFolder());
    }

    public void save(FishSpecies species) {
        writer.save(species);
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
