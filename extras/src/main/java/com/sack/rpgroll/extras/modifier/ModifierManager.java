package com.sack.rpgroll.extras.modifier;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Carga plugins/RPGRoll-Extras/modifiers/*.yml — un archivo por raza/clase/job que aporta modificadores. */
public class ModifierManager extends ContentManager<ModifierSet> {

    public ModifierManager(JavaPlugin extrasPlugin) {
        super(resolveCoreInstance(), new YamlLoader(extrasPlugin), "modifiers", "modificador", new ModifierParser());
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}
