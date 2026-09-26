package com.sack.rpgroll.items.ore;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Las menas de {@code ores/}. Es opcional: un servidor sin menas propias no avisa de nada. */
public class OreManager extends ContentManager<OreDefinition> {

    public OreManager(JavaPlugin itemsPlugin) {
        super(JavaPlugin.getProvidingPlugin(OreManager.class), new YamlLoader(itemsPlugin), "ores", "mena",
                new OreParser());
    }

    @Override
    protected boolean optionalContent() {
        return true;
    }

}
