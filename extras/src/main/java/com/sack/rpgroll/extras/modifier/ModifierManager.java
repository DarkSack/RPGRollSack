package com.sack.rpgroll.extras.modifier;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Carga plugins/RPGRoll-Extras/modifiers/*.yml — un archivo por raza/clase/job que aporta modificadores. */
public class ModifierManager extends ContentManager<ModifierSet> {

    public ModifierManager(JavaPlugin extrasPlugin) {
        super(owningPlugin(), new YamlLoader(extrasPlugin), "modifiers", "modificador", new ModifierParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(ModifierManager.class);
    }

}
