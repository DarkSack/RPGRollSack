package com.sack.rpgroll.tab.tablist;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de tablist desde plugins/RPGRoll-TAB/tablists/*.yml. */
public class TablistManager extends ContentManager<TablistDefinition> {

    public TablistManager(JavaPlugin tabPlugin) {
        super(resolveCoreInstance(), new YamlLoader(tabPlugin), "tablists", "tablist", new TablistParser());
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}
