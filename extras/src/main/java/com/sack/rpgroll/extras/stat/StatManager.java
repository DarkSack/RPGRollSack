package com.sack.rpgroll.extras.stat;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de need/attribute desde plugins/RPGRoll-Extras/stats/*.yml. */
public class StatManager extends ContentManager<StatDefinition> {

    public StatManager(JavaPlugin extrasPlugin) {
        super(resolveCoreInstance(), new YamlLoader(extrasPlugin), "stats", "stat", new StatParser());
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}
