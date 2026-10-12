package com.sack.rpgroll.quests.region;

import com.sack.rpgroll.common.yaml.YamlSaver;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class RegionDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public RegionDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(Region region) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", region.id());
        config.set("world", region.world());
        config.set("min.x", region.minX());
        config.set("min.y", region.minY());
        config.set("min.z", region.minZ());
        config.set("max.x", region.maxX());
        config.set("max.y", region.maxY());
        config.set("max.z", region.maxZ());

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            YamlSaver.save(config, new File(folder, region.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando región '" + region.id() + "': " + e.getMessage());
        }
    }

}
