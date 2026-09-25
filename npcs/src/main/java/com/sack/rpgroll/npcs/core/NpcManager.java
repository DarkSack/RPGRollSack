package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.common.yaml.YamlLoader;
import com.sack.rpgroll.common.content.ContentManager;

import org.bukkit.plugin.java.JavaPlugin;

public class NpcManager extends ContentManager<NpcDefinition> {

    public NpcManager(JavaPlugin npcsPlugin) {
        super(owningPlugin(), new YamlLoader(npcsPlugin), "npcs", "npc", new NpcParser());
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(NpcManager.class);
    }

}