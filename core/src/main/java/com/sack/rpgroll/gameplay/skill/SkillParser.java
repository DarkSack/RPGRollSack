package com.sack.rpgroll.gameplay.skill;

import com.sack.rpgroll.common.content.ContentParser;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Convierte un YamlConfiguration (skills/fireball.yml, etc.) en una instancia
 * de Skill.
 */
public class SkillParser implements ContentParser<Skill> {

    @Override
    public Skill parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String name = config.getString("name", id);
        String description = config.getString("description", "");
        int requiredLevel = config.getInt("required-level", 1);
        int manaCost = config.getInt("mana-cost", 0);
        int cooldownSeconds = config.getInt("cooldown-seconds", 0);
        double damageMultiplier = config.getDouble("damage-multiplier", 1.0);

        return new Skill(id, name, description, requiredLevel, manaCost, cooldownSeconds, damageMultiplier);
    }

}