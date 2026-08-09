package com.sack.rpgroll.workers.core.profession;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProfessionDefinitionWriter {

    private final File folder;

    public ProfessionDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "professions");
    }

    public void save(Profession profession) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", profession.id());
        config.set("display-name", profession.displayName());
        config.set("icon", profession.icon());
        config.set("description", profession.description());
        config.set("entity-type", profession.entityType());
        config.set("skills", List.copyOf(profession.skillIds()));
        config.set("schedule", profession.scheduleId());
        config.set("wage-amount", profession.wageAmount());
        config.set("wage-type", profession.wageType().name());
        config.set("tool-material", profession.toolMaterial());

        List<Map<String, Object>> rules = new java.util.ArrayList<>();

        for (AiRule rule : profession.aiRules()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("condition", rule.condition().name());
            map.put("action", rule.action().name());
            map.put("priority", rule.priority());
            rules.add(map);
        }

        config.set("ai-rules", rules);

        try {
            folder.mkdirs();
            config.save(new File(folder, profession.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la profesión " + profession.id(), e);
        }
    }

}
