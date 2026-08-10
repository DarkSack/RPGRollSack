package com.sack.rpgroll.extras.condition;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de condition desde plugins/RPGRoll-Extras/conditions/*.yml. */
public class ConditionManager extends ContentManager<ConditionDefinition> {

    public ConditionManager(JavaPlugin extrasPlugin) {
        super(resolveCoreInstance(), new YamlLoader(extrasPlugin), "conditions", "condition", new ConditionParser());
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}
