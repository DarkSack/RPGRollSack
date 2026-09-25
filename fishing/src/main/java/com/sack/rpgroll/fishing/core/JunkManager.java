package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class JunkManager extends ContentManager<Junk> {

    private final JunkDefinitionWriter writer;

    public JunkManager(JavaPlugin fishingPlugin) {
        super(owningPlugin(), new YamlLoader(fishingPlugin), "junk", "basura", new JunkParser());
        this.writer = new JunkDefinitionWriter(fishingPlugin.getDataFolder());
    }

    public void save(Junk junk) {
        writer.save(junk);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(JunkManager.class);
    }

}
