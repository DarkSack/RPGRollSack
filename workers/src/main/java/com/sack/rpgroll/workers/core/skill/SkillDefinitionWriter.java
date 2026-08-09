package com.sack.rpgroll.workers.core.skill;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class SkillDefinitionWriter {

    private final File folder;

    public SkillDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "skills");
    }

    public void save(Skill skill) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", skill.id());
        config.set("display-name", skill.displayName());
        config.set("description", skill.description());
        config.set("profession", skill.professionId());
        config.set("max-level", skill.maxLevel());
        config.set("attribute-key", skill.attributeKey());
        config.set("value-per-level", skill.valuePerLevel());

        try {
            folder.mkdirs();
            config.save(new File(folder, skill.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la habilidad " + skill.id(), e);
        }
    }

}
