package com.sack.rpgroll.items.socket;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class GemManager extends ContentManager<Gem> {

    public GemManager(JavaPlugin itemsPlugin) {
        super(owningPlugin(), new YamlLoader(itemsPlugin), "gems", "gema", new GemParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(GemManager.class);
    }

}
