package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class SeasonManager extends ContentManager<Season> {

    private final SeasonDefinitionWriter writer;

    public SeasonManager(JavaPlugin seasonsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(seasonsPlugin), "seasons", "estación", new SeasonParser());
        this.writer = new SeasonDefinitionWriter(seasonsPlugin.getDataFolder());
    }

    public void save(Season season) {
        writer.save(season);
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
