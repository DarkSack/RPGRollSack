package com.sack.rpgroll.gameplay.job;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/** Serializa un {@link Job} completo de vuelta a YAML — inverso de {@link JobParser}. */
public class JobDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public JobDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(Job job) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", job.id());
        config.set("display-name", job.displayName());
        config.set("description", job.description());
        config.set("icon", job.icon());
        config.set("lore", job.lore());
        config.set("max-level", job.maxLevel());
        config.set("exp-base", job.expBase());
        config.set("exp-multiplier", job.expMultiplier());
        config.set("new-biome-money", job.newBiomeMoney());
        config.set("new-biome-experience", job.newBiomeExperience());
        config.set("distance-blocks", job.distanceBlocks());
        config.set("distance-money", job.distanceMoney());
        config.set("distance-experience", job.distanceExperience());

        for (var entry : job.rewards().entrySet()) {
            config.set("rewards." + entry.getKey() + ".money", entry.getValue().money());
            config.set("rewards." + entry.getKey() + ".experience", entry.getValue().experience());
        }

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, job.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando trabajo '" + job.id() + "': " + e.getMessage());
        }
    }

}
