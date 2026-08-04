package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;

public class FishingRegionParser implements ContentParser<FishingRegion> {

    @Override
    public FishingRegion parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String world = config.getString("world");
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("región '" + id + "' sin campo obligatorio 'world'");
        }

        WaterType waterType;

        try {
            waterType = WaterType.valueOf(
                    config.getString("forced-water-type", "MAGIC_WATER").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            waterType = WaterType.MAGIC_WATER;
        }

        return new FishingRegion(id, world,
                config.getDouble("min-x"), config.getDouble("min-y"), config.getDouble("min-z"),
                config.getDouble("max-x"), config.getDouble("max-y"), config.getDouble("max-z"), waterType);
    }

}
