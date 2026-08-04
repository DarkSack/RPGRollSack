package com.sack.rpgroll.race;

import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.api.stats.StatType;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/** Serializa una {@link Race} completa de vuelta a YAML — inverso de {@link RaceParser}. */
public class RaceDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public RaceDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(Race race) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", race.id());
        config.set("display-name", race.displayName());
        config.set("description", race.description());
        config.set("icon", race.icon());
        config.set("passive-traits", race.passiveTraits());
        config.set("lore", race.lore());

        for (var entry : race.baseAttributes().entrySet()) {
            config.set("base-attributes." + entry.getKey().name().toLowerCase(), entry.getValue());
        }

        var physical = race.physicalModifiers();
        config.set("physical.scale", physical.scale());
        config.set("physical.movement-speed-percent", physical.movementSpeedPercent());
        config.set("physical.extra-health", physical.extraHealth());
        config.set("physical.knockback-resistance", physical.knockbackResistance());

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, race.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando raza '" + race.id() + "': " + e.getMessage());
        }
    }

}
