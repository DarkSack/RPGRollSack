package com.sack.rpgroll.traps.turret;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Carga las torretas desde plugins/RPGRoll-Traps/turrets/*.yml — mismo
 * framework de contenido de :common que {@code TrapManager}, en un folder
 * separado porque una torreta no es una TrapDefinition.
 */
public class TurretManager extends ContentManager<TurretDefinition> {

    private final TurretDefinitionWriter writer;

    public TurretManager(JavaPlugin trapsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(trapsPlugin), "turrets", "torreta", new TurretParser());
        this.writer = new TurretDefinitionWriter(new File(trapsPlugin.getDataFolder(), "turrets"),
                trapsPlugin.getLogger());
    }

    public void save(TurretDefinition turret) {
        writer.save(turret);
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
