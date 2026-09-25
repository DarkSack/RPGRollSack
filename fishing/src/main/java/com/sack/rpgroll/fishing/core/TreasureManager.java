package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class TreasureManager extends ContentManager<Treasure> {

    private final TreasureDefinitionWriter writer;

    public TreasureManager(JavaPlugin fishingPlugin) {
        super(owningPlugin(), new YamlLoader(fishingPlugin), "treasures", "tesoro", new TreasureParser());
        this.writer = new TreasureDefinitionWriter(fishingPlugin.getDataFolder());
    }

    public void save(Treasure treasure) {
        writer.save(treasure);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(TreasureManager.class);
    }

}
