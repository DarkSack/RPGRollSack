package com.sack.rpgroll.gameplay.skill;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/** Serializa un {@link Skill} completo de vuelta a YAML — inverso de {@link SkillParser}. */
public class SkillDefinitionWriter {

    private final File folder;
    private final Logger logger;

    public SkillDefinitionWriter(File folder, Logger logger) {
        this.folder = folder;
        this.logger = logger;
    }

    public void save(Skill skill) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", skill.id());
        config.set("name", skill.name());
        config.set("description", skill.description());
        config.set("required-level", skill.requiredLevel());
        config.set("mana-cost", skill.manaCost());
        config.set("cooldown-seconds", skill.cooldownSeconds());
        config.set("damage-multiplier", skill.damageMultiplier());

        try {
            if (!folder.exists()) {
                folder.mkdirs();
            }
            config.save(new File(folder, skill.id() + ".yml"));
        } catch (IOException e) {
            logger.warning("✘ Error guardando skill '" + skill.id() + "': " + e.getMessage());
        }
    }

}
