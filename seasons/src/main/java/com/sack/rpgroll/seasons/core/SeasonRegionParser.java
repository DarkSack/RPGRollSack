package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;

public class SeasonRegionParser implements ContentParser<SeasonRegion> {

    @Override
    public SeasonRegion parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String world = config.getString("world");
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("región '" + id + "' sin campo obligatorio 'world'");
        }

        SeasonRegionOverrideMode mode;

        try {
            mode = SeasonRegionOverrideMode.valueOf(
                    config.getString("override-mode", "FOLLOW_WORLD_CALENDAR").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            mode = SeasonRegionOverrideMode.FOLLOW_WORLD_CALENDAR;
        }

        return new SeasonRegion(id, world,
                config.getDouble("min-x"), config.getDouble("min-y"), config.getDouble("min-z"),
                config.getDouble("max-x"), config.getDouble("max-y"), config.getDouble("max-z"),
                mode, config.getString("pinned-season"), config.getString("pinned-calendar"));
    }

}
