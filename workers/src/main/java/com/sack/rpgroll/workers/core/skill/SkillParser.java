package com.sack.rpgroll.workers.core.skill;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class SkillParser implements ContentParser<Skill> {

    @Override
    public Skill parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String professionId = config.getString("profession");
        if (professionId == null || professionId.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'profession'");
        }

        return new Skill(
                id,
                config.getString("display-name", id),
                config.getString("description", ""),
                professionId,
                config.getInt("max-level", 10),
                config.getString("attribute-key", id),
                config.getDouble("value-per-level", 1.0));
    }

}
