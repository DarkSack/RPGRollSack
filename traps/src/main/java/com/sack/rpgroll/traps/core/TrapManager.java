package com.sack.rpgroll.traps.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

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
        super(owningPlugin(), new YamlLoader(trapsPlugin), "traps", "trampa", new TrapParser());
        this.writer = new TrapDefinitionWriter(new File(trapsPlugin.getDataFolder(), "traps"),
                trapsPlugin.getLogger());
    }

    /** Persiste la trampa a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(TrapDefinition trap) {
        writer.save(trap);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(TrapManager.class);
    }

}
