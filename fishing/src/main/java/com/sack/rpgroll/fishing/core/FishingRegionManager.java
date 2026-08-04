package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class FishingRegionManager extends ContentManager<FishingRegion> {

    private final FishingRegionDefinitionWriter writer;

    public FishingRegionManager(JavaPlugin fishingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(fishingPlugin), "regions", "región", new FishingRegionParser());
        this.writer = new FishingRegionDefinitionWriter(fishingPlugin.getDataFolder());
    }

    public void save(FishingRegion region) {
        writer.save(region);
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
