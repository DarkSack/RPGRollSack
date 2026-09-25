package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class SeasonManager extends ContentManager<Season> {

    private final SeasonDefinitionWriter writer;

    public SeasonManager(JavaPlugin seasonsPlugin) {
        super(owningPlugin(), new YamlLoader(seasonsPlugin), "seasons", "estación", new SeasonParser());
        this.writer = new SeasonDefinitionWriter(seasonsPlugin.getDataFolder());
    }

    public void save(Season season) {
        writer.save(season);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(SeasonManager.class);
    }

}
