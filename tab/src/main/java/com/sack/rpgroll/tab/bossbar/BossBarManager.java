package com.sack.rpgroll.tab.bossbar;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de bossbar desde plugins/RPGRoll-TAB/bossbars/*.yml. */
public class BossBarManager extends ContentManager<BossBarDefinition> {

    public BossBarManager(JavaPlugin tabPlugin) {
        super(resolveCoreInstance(), new YamlLoader(tabPlugin), "bossbars", "bossbar", new BossBarParser());
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}
