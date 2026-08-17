package com.sack.rpgroll.ranching.core.breeds;

import com.sack.rpgroll.common.content.ContentParser;
import com.sack.rpgroll.common.reskin.EntityReskinYaml;

import org.bukkit.configuration.file.YamlConfiguration;

public class BreedParser implements ContentParser<Breed> {

    @Override
    public Breed parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String speciesId = config.getString("species");
        if (speciesId == null || speciesId.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'species'");
        }

        return new Breed(
                id,
                config.getString("display-name", id),
                config.getString("description", ""),
                speciesId,
                config.getDouble("production-multiplier", 1.0),
                config.getDouble("weight-multiplier", 1.0),
                config.getDouble("fertility-multiplier", 1.0),
                config.getDouble("resistance-multiplier", 1.0),
                config.getString("temperament", "Neutral"),
                EntityReskinYaml.parse(config));
    }

}
