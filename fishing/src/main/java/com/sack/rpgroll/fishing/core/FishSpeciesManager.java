package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class FishSpeciesManager extends ContentManager<FishSpecies> {

    private final FishSpeciesDefinitionWriter writer;

    public FishSpeciesManager(JavaPlugin fishingPlugin) {
        super(owningPlugin(), new YamlLoader(fishingPlugin), "species", "especie", new FishSpeciesParser());
        this.writer = new FishSpeciesDefinitionWriter(fishingPlugin.getDataFolder());
    }

    public void save(FishSpecies species) {
        writer.save(species);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(FishSpeciesManager.class);
    }

}
