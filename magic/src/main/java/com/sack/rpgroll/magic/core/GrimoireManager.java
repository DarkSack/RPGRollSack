package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class GrimoireManager extends ContentManager<Grimoire> {

    private final GrimoireDefinitionWriter writer;

    public GrimoireManager(JavaPlugin magicPlugin) {
        super(resolveCoreInstance(), new YamlLoader(magicPlugin), "grimoires", "grimorio", new GrimoireParser());
        this.writer = new GrimoireDefinitionWriter(magicPlugin.getDataFolder());
    }

    public void save(Grimoire grimoire) {
        writer.save(grimoire);
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
