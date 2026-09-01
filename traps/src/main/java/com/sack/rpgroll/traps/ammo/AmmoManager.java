package com.sack.rpgroll.traps.ammo;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Carga las municiones desde plugins/RPGRoll-Traps/ammo/*.yml. */
public class AmmoManager extends ContentManager<AmmoDefinition> {

    public AmmoManager(JavaPlugin trapsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(trapsPlugin), "ammo", "munición", new AmmoParser());
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}
