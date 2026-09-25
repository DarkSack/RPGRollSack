package com.sack.rpgroll.tab.sorting;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de orden desde plugins/RPGRoll-TAB/sortings/*.yml. */
public class SortingManager extends ContentManager<SortingDefinition> {

    public SortingManager(JavaPlugin tabPlugin) {
        super(owningPlugin(), new YamlLoader(tabPlugin), "sortings", "orden", new SortingParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(SortingManager.class);
    }

}
