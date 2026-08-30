package com.sack.rpgroll.traps.core;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Carga los tipos de trampa desde plugins/RPGRoll-Traps/traps/*.yml usando
 * el framework genérico de contenido de :common. Mismo patrón que
 * CrateManager en RPGRoll-Crates.
 */
public class TrapManager extends ContentManager<TrapDefinition> {

    private final TrapDefinitionWriter writer;

    public TrapManager(JavaPlugin trapsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(trapsPlugin), "traps", "trampa", new TrapParser());
        this.writer = new TrapDefinitionWriter(new File(trapsPlugin.getDataFolder(), "traps"),
                trapsPlugin.getLogger());
    }

    /** Persiste la trampa a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(TrapDefinition trap) {
        writer.save(trap);
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
