package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class FactionManager extends ContentManager<Faction> {

    private final FactionDefinitionWriter writer;

    public FactionManager(JavaPlugin ascensionPlugin) {
        super(owningPlugin(), new YamlLoader(ascensionPlugin), "factions", "facción", new FactionParser());
        this.writer = new FactionDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(Faction faction) {
        writer.save(faction);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(FactionManager.class);
    }

}
