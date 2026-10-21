package com.sack.rpgroll.disasters.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Los desastres de {@code disasters/}. Se editan en YAML. */
public class DisasterManager extends ContentManager<Disaster> {

    public DisasterManager(JavaPlugin plugin) {
        super(JavaPlugin.getProvidingPlugin(DisasterManager.class), new YamlLoader(plugin), "disasters",
                "desastre natural", new DisasterParser());
    }

}
