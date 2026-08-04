package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldEventManager extends ContentManager<WorldEvent> {

    private final WorldEventDefinitionWriter writer;

    public WorldEventManager(JavaPlugin seasonsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(seasonsPlugin), "events", "evento mundial",
                new WorldEventParser());
        this.writer = new WorldEventDefinitionWriter(seasonsPlugin.getDataFolder());
    }

    public void save(WorldEvent event) {
        writer.save(event);
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
