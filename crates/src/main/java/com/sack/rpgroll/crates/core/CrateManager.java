package com.sack.rpgroll.crates.core;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Carga los tipos de crate desde plugins/RPGRoll-Crates/crates/*.yml,
 * usando el framework genérico de contenido de :common. Mismo patrón que
 * NpcManager en el addon de NPCs.
 */
public class CrateManager extends ContentManager<Crate> {

    private final CrateDefinitionWriter writer;

    public CrateManager(JavaPlugin cratesPlugin) {
        super(resolveCoreInstance(), new YamlLoader(cratesPlugin), "crates", "crate", new CrateParser());
        this.writer = new CrateDefinitionWriter(new File(cratesPlugin.getDataFolder(), "crates"),
                cratesPlugin.getLogger());
    }

    /** Persiste el crate a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(Crate crate) {
        writer.save(crate);
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
