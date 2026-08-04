package com.sack.rpgroll.mobs.region;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class MobRegionParser implements ContentParser<MobRegion> {

    @Override
    public MobRegion parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String world = config.getString("world", "world");

        return new MobRegion(id, world,
                config.getDouble("min.x"), config.getDouble("min.y"), config.getDouble("min.z"),
                config.getDouble("max.x"), config.getDouble("max.y"), config.getDouble("max.z"));
    }

}
