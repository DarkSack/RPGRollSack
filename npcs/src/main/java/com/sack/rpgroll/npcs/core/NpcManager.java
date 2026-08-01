package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.yaml.YamlLoader;
import com.sack.rpgroll.common.content.ContentManager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class NpcManager extends ContentManager<NpcDefinition> {

    public NpcManager(JavaPlugin npcsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(npcsPlugin), "npcs", "npc", new NpcParser());
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}