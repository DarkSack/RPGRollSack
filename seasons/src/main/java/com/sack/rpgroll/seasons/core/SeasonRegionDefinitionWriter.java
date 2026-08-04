package com.sack.rpgroll.seasons.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class SeasonRegionDefinitionWriter {

    private final File folder;

    public SeasonRegionDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "regions");
    }

    public void save(SeasonRegion region) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", region.id());
        config.set("world", region.world());
        config.set("min-x", region.minX());
        config.set("min-y", region.minY());
        config.set("min-z", region.minZ());
        config.set("max-x", region.maxX());
        config.set("max-y", region.maxY());
        config.set("max-z", region.maxZ());
        config.set("override-mode", region.overrideMode().name());
        config.set("pinned-season", region.pinnedSeasonId());
        config.set("pinned-calendar", region.pinnedCalendarId());

        try {
            folder.mkdirs();
            config.save(new File(folder, region.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la región " + region.id(), e);
        }
    }

}
