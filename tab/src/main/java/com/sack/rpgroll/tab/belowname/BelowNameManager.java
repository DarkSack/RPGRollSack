package com.sack.rpgroll.tab.belowname;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de belowname desde plugins/RPGRoll-TAB/belownames/*.yml. */
public class BelowNameManager extends ContentManager<BelowNameDefinition> {

    public BelowNameManager(JavaPlugin tabPlugin) {
        super(owningPlugin(), new YamlLoader(tabPlugin), "belownames", "belowname", new BelowNameParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(BelowNameManager.class);
    }

}
