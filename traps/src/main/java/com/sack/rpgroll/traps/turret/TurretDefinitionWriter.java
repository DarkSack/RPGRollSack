package com.sack.rpgroll.traps.turret;

import com.sack.rpgroll.common.yaml.YamlSaver;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/** Serializa un {@link TurretDefinition} completo de vuelta a YAML — inverso de {@link TurretParser}. */
public class TurretDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public TurretDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(TurretDefinition turret) {

        YamlConfiguration config = new YamlConfiguration();

        config.set("id", turret.id());
        config.set("display-name", turret.displayName());
        config.set("description", turret.description());
        config.set("radius", turret.radius());
        config.set("target-players", turret.targetPlayers());
        config.set("target-hostile-mobs", turret.targetHostileMobs());
        config.set("fire-interval-ticks", turret.fireIntervalTicks());
        config.set("conditions", turret.conditions());

        config.set("impact.type", turret.impact().type());
        for (var entry : turret.impact().params().entrySet()) {
            config.set("impact.params." + entry.getKey(), entry.getValue());
        }

        config.set("model.material", turret.model().name());
        config.set("model.custom-model-data", turret.customModelData());
        var itemModel = com.sack.rpgroll.traps.item.ItemModels.get("turret", turret.id());
        config.set("model.item-model", itemModel == null ? null : itemModel.toString());

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            YamlSaver.save(config, new File(folder, turret.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando torreta '" + turret.id() + "': " + e.getMessage());
        }
    }

}
