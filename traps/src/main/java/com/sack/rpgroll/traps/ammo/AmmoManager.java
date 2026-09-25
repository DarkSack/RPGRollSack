package com.sack.rpgroll.traps.ammo;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Carga las municiones desde plugins/RPGRoll-Traps/ammo/*.yml. */
public class AmmoManager extends ContentManager<AmmoDefinition> {

    public AmmoManager(JavaPlugin trapsPlugin) {
        super(owningPlugin(), new YamlLoader(trapsPlugin), "ammo", "munición", new AmmoParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(AmmoManager.class);
    }

}
