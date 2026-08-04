package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class AffinityManager extends ContentManager<Affinity> {

    private final AffinityDefinitionWriter writer;

    public AffinityManager(JavaPlugin ascensionPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ascensionPlugin), "affinities", "afinidad", new AffinityParser());
        this.writer = new AffinityDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(Affinity affinity) {
        writer.save(affinity);
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
