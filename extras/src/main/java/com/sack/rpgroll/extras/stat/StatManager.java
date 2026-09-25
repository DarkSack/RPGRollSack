package com.sack.rpgroll.extras.stat;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de need/attribute desde plugins/RPGRoll-Extras/stats/*.yml. */
public class StatManager extends ContentManager<StatDefinition> {

    public StatManager(JavaPlugin extrasPlugin) {
        super(owningPlugin(), new YamlLoader(extrasPlugin), "stats", "stat", new StatParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(StatManager.class);
    }

}
