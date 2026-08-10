package com.sack.rpgroll.tab.teams;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de team desde plugins/RPGRoll-TAB/teams/*.yml. */
public class TeamsManager extends ContentManager<TeamsDefinition> {

    public TeamsManager(JavaPlugin tabPlugin) {
        super(resolveCoreInstance(), new YamlLoader(tabPlugin), "teams", "team", new TeamsParser());
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}
