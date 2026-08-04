package com.sack.rpgroll.quests.region;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class RegionParser implements ContentParser<Region> {

    @Override
    public Region parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String world = config.getString("world", "world");

        double minX = config.getDouble("min.x");
        double minY = config.getDouble("min.y");
        double minZ = config.getDouble("min.z");
        double maxX = config.getDouble("max.x");
        double maxY = config.getDouble("max.y");
        double maxZ = config.getDouble("max.z");

        return new Region(id, world, minX, minY, minZ, maxX, maxY, maxZ);
    }

}
