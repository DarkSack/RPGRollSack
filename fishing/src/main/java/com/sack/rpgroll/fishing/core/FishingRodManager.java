package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class FishingRodManager extends ContentManager<FishingRod> {

    private final FishingRodDefinitionWriter writer;

    public FishingRodManager(JavaPlugin fishingPlugin) {
        super(owningPlugin(), new YamlLoader(fishingPlugin), "rods", "caña", new FishingRodParser());
        this.writer = new FishingRodDefinitionWriter(fishingPlugin.getDataFolder());
    }

    public void save(FishingRod rod) {
        writer.save(rod);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(FishingRodManager.class);
    }

}
