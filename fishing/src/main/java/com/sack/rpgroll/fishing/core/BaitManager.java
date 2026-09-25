package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class BaitManager extends ContentManager<Bait> {

    private final BaitDefinitionWriter writer;

    public BaitManager(JavaPlugin fishingPlugin) {
        super(owningPlugin(), new YamlLoader(fishingPlugin), "baits", "carnada", new BaitParser());
        this.writer = new BaitDefinitionWriter(fishingPlugin.getDataFolder());
    }

    public void save(Bait bait) {
        writer.save(bait);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(BaitManager.class);
    }

}
