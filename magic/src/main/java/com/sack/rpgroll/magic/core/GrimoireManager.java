package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class GrimoireManager extends ContentManager<Grimoire> {

    private final GrimoireDefinitionWriter writer;

    public GrimoireManager(JavaPlugin magicPlugin) {
        super(owningPlugin(), new YamlLoader(magicPlugin), "grimoires", "grimorio", new GrimoireParser());
        this.writer = new GrimoireDefinitionWriter(magicPlugin.getDataFolder());
    }

    public void save(Grimoire grimoire) {
        writer.save(grimoire);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(GrimoireManager.class);
    }

}
