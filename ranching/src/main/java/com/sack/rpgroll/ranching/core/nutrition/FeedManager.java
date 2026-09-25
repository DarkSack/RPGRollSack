package com.sack.rpgroll.ranching.core.nutrition;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class FeedManager extends ContentManager<Feed> {

    private final FeedDefinitionWriter writer;

    public FeedManager(JavaPlugin ranchingPlugin) {
        super(owningPlugin(), new YamlLoader(ranchingPlugin), "feeds", "alimento", new FeedParser());
        this.writer = new FeedDefinitionWriter(ranchingPlugin.getDataFolder());
    }

    public void save(Feed feed) {
        writer.save(feed);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(FeedManager.class);
    }

}
