package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class SeasonRegionManager extends ContentManager<SeasonRegion> {

    private final SeasonRegionDefinitionWriter writer;

    public SeasonRegionManager(JavaPlugin seasonsPlugin) {
        super(owningPlugin(), new YamlLoader(seasonsPlugin), "regions", "región", new SeasonRegionParser());
        this.writer = new SeasonRegionDefinitionWriter(seasonsPlugin.getDataFolder());
    }

    public void save(SeasonRegion region) {
        writer.save(region);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(SeasonRegionManager.class);
    }

}
