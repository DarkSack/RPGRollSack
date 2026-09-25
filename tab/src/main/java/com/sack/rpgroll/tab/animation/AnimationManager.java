package com.sack.rpgroll.tab.animation;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Carga las animaciones reusables desde plugins/RPGRoll-TAB/animations/*.yml. */
public class AnimationManager extends ContentManager<AnimationDefinition> {

    public AnimationManager(JavaPlugin tabPlugin) {
        super(owningPlugin(), new YamlLoader(tabPlugin), "animations", "animación", new AnimationParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(AnimationManager.class);
    }

}
