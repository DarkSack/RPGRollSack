package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class SeasonRegionManager extends ContentManager<SeasonRegion> {

    private final SeasonRegionDefinitionWriter writer;

    public SeasonRegionManager(JavaPlugin seasonsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(seasonsPlugin), "regions", "región", new SeasonRegionParser());
        this.writer = new SeasonRegionDefinitionWriter(seasonsPlugin.getDataFolder());
    }

    public void save(SeasonRegion region) {
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
