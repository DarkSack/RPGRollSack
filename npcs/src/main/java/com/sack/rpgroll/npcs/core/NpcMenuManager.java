package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.yaml.YamlLoader;
import com.sack.rpgroll.common.content.ContentManager;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class NpcMenuManager extends ContentManager<NpcMenuDefinition> {

    private final NpcMenuWriter writer;

    public NpcMenuManager(JavaPlugin npcsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(npcsPlugin), "menus", "menú", new NpcMenuParser());
        this.writer = new NpcMenuWriter(npcsPlugin);
    }

    /** Persiste el menú a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(NpcMenuDefinition definition) {
        writer.save(definition);
        reload();
    }

    private static RPGRoll resolveCoreInstance() {
        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");
        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }
        return rpgRoll;
    }

}