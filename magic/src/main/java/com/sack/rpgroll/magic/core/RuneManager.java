package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class RuneManager extends ContentManager<Rune> {

    private final RuneDefinitionWriter writer;

    public RuneManager(JavaPlugin magicPlugin) {
        super(resolveCoreInstance(), new YamlLoader(magicPlugin), "runes", "runa", new RuneParser());
        this.writer = new RuneDefinitionWriter(magicPlugin.getDataFolder());
    }

    public void save(Rune rune) {
        writer.save(rune);
        reload();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
