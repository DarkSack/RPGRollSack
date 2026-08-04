package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionManager extends ContentManager<Faction> {

    private final FactionDefinitionWriter writer;

    public FactionManager(JavaPlugin ascensionPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ascensionPlugin), "factions", "facción", new FactionParser());
        this.writer = new FactionDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(Faction faction) {
        writer.save(faction);
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
