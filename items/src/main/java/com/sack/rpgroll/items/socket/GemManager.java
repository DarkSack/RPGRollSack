package com.sack.rpgroll.items.socket;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class GemManager extends ContentManager<Gem> {

    public GemManager(JavaPlugin itemsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(itemsPlugin), "gems", "gema", new GemParser());
    }

    private static JavaPlugin resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
