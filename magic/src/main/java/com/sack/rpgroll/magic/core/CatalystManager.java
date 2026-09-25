package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class CatalystManager extends ContentManager<SpellCatalyst> {

    private final CatalystDefinitionWriter writer;

    public CatalystManager(JavaPlugin magicPlugin) {
        super(owningPlugin(), new YamlLoader(magicPlugin), "catalysts", "catalizador", new CatalystParser());
        this.writer = new CatalystDefinitionWriter(magicPlugin.getDataFolder());
    }

    public void save(SpellCatalyst catalyst) {
        writer.save(catalyst);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(CatalystManager.class);
    }

}
