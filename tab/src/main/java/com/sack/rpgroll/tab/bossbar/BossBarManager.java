package com.sack.rpgroll.tab.bossbar;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de bossbar desde plugins/RPGRoll-TAB/bossbars/*.yml. */
public class BossBarManager extends ContentManager<BossBarDefinition> {

    public BossBarManager(JavaPlugin tabPlugin) {
        super(owningPlugin(), new YamlLoader(tabPlugin), "bossbars", "bossbar", new BossBarParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(BossBarManager.class);
    }

}
