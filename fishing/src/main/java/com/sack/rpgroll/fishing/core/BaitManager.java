package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BaitManager extends ContentManager<Bait> {

    private final BaitDefinitionWriter writer;

    public BaitManager(JavaPlugin fishingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(fishingPlugin), "baits", "carnada", new BaitParser());
        this.writer = new BaitDefinitionWriter(fishingPlugin.getDataFolder());
    }

    public void save(Bait bait) {
        writer.save(bait);
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
