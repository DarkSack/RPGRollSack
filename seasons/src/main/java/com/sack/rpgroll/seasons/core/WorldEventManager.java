package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class WorldEventManager extends ContentManager<WorldEvent> {

    private final WorldEventDefinitionWriter writer;

    public WorldEventManager(JavaPlugin seasonsPlugin) {
        super(owningPlugin(), new YamlLoader(seasonsPlugin), "events", "evento mundial",
                new WorldEventParser());
        this.writer = new WorldEventDefinitionWriter(seasonsPlugin.getDataFolder());
    }

    public void save(WorldEvent event) {
        writer.save(event);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(WorldEventManager.class);
    }

}
