package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Los desastres de {@code disasters/}. Se editan en YAML; no tienen editor en el Season Studio. */
public class DisasterManager extends ContentManager<Disaster> {

    public DisasterManager(JavaPlugin seasonsPlugin) {
        super(JavaPlugin.getProvidingPlugin(DisasterManager.class), new YamlLoader(seasonsPlugin), "disasters",
                "desastre natural", new DisasterParser());
    }

}
