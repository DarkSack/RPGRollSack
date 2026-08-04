package com.sack.rpgroll.ranching.core.nutrition;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class FeedManager extends ContentManager<Feed> {

    private final FeedDefinitionWriter writer;

    public FeedManager(JavaPlugin ranchingPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ranchingPlugin), "feeds", "alimento", new FeedParser());
        this.writer = new FeedDefinitionWriter(ranchingPlugin.getDataFolder());
    }

    public void save(Feed feed) {
        writer.save(feed);
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
