package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class AffinityParser implements ContentParser<Affinity> {

    @Override
    public Affinity parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String opposingId = config.getString("opposing");

        return new Affinity(id, displayName, opposingId, config.getStringList("resist-causes"));
    }

}
