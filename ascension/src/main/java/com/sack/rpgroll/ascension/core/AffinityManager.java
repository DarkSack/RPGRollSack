package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class AffinityManager extends ContentManager<Affinity> {

    private final AffinityDefinitionWriter writer;

    public AffinityManager(JavaPlugin ascensionPlugin) {
        super(owningPlugin(), new YamlLoader(ascensionPlugin), "affinities", "afinidad", new AffinityParser());
        this.writer = new AffinityDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(Affinity affinity) {
        writer.save(affinity);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(AffinityManager.class);
    }

}
