package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class FishingRegionManager extends ContentManager<FishingRegion> {

    private final FishingRegionDefinitionWriter writer;

    public FishingRegionManager(JavaPlugin fishingPlugin) {
        super(owningPlugin(), new YamlLoader(fishingPlugin), "regions", "región", new FishingRegionParser());
        this.writer = new FishingRegionDefinitionWriter(fishingPlugin.getDataFolder());
    }

    public void save(FishingRegion region) {
        writer.save(region);
        reload();
    }

    @Override
    protected boolean optionalContent() {
        return true;
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(FishingRegionManager.class);
    }

}
