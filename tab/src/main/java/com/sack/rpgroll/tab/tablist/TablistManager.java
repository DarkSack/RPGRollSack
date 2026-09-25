package com.sack.rpgroll.tab.tablist;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de tablist desde plugins/RPGRoll-TAB/tablists/*.yml. */
public class TablistManager extends ContentManager<TablistDefinition> {

    public TablistManager(JavaPlugin tabPlugin) {
        super(owningPlugin(), new YamlLoader(tabPlugin), "tablists", "tablist", new TablistParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(TablistManager.class);
    }

}
