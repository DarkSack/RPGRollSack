package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class RuneManager extends ContentManager<Rune> {

    private final RuneDefinitionWriter writer;

    public RuneManager(JavaPlugin magicPlugin) {
        super(owningPlugin(), new YamlLoader(magicPlugin), "runes", "runa", new RuneParser());
        this.writer = new RuneDefinitionWriter(magicPlugin.getDataFolder());
    }

    public void save(Rune rune) {
        writer.save(rune);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(RuneManager.class);
    }

}
