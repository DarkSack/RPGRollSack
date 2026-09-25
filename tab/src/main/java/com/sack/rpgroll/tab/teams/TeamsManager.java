package com.sack.rpgroll.tab.teams;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de team desde plugins/RPGRoll-TAB/teams/*.yml. */
public class TeamsManager extends ContentManager<TeamsDefinition> {

    public TeamsManager(JavaPlugin tabPlugin) {
        super(owningPlugin(), new YamlLoader(tabPlugin), "teams", "team", new TeamsParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(TeamsManager.class);
    }

}
