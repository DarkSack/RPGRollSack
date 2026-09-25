package com.sack.rpgroll.extras.condition;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Carga las definiciones de condition desde plugins/RPGRoll-Extras/conditions/*.yml. */
public class ConditionManager extends ContentManager<ConditionDefinition> {

    public ConditionManager(JavaPlugin extrasPlugin) {
        super(owningPlugin(), new YamlLoader(extrasPlugin), "conditions", "condition", new ConditionParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(ConditionManager.class);
    }

}
