package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class FishingRodManager extends ContentManager<FishingRod> {

    private final FishingRodDefinitionWriter writer;

    public FishingRodManager(JavaPlugin fishingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(fishingPlugin), "rods", "caña", new FishingRodParser());
        this.writer = new FishingRodDefinitionWriter(fishingPlugin.getDataFolder());
    }

    public void save(FishingRod rod) {
        writer.save(rod);
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
