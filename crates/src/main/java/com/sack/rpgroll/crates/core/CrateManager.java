package com.sack.rpgroll.crates.core;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Carga los tipos de crate desde plugins/RPGRoll-Crates/crates/*.yml,
 * usando el framework genérico de contenido de :common. Mismo patrón que
 * NpcManager en el addon de NPCs.
 */
public class CrateManager extends ContentManager<Crate> {

    public CrateManager(JavaPlugin cratesPlugin) {
        super(resolveCoreInstance(), new YamlLoader(cratesPlugin), "crates", "crate", new CrateParser());
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}
