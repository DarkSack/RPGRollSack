package com.sack.rpgroll.tab.nametag;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de nametag desde plugins/RPGRoll-TAB/nametags/*.yml. */
public class NametagManager extends ContentManager<NametagDefinition> {

    public NametagManager(JavaPlugin tabPlugin) {
        super(owningPlugin(), new YamlLoader(tabPlugin), "nametags", "nametag", new NametagParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(NametagManager.class);
    }

}
