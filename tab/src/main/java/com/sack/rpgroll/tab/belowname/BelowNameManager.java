package com.sack.rpgroll.tab.belowname;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de belowname desde plugins/RPGRoll-TAB/belownames/*.yml. */
public class BelowNameManager extends ContentManager<BelowNameDefinition> {

    public BelowNameManager(JavaPlugin tabPlugin) {
        super(resolveCoreInstance(), new YamlLoader(tabPlugin), "belownames", "belowname", new BelowNameParser());
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}
