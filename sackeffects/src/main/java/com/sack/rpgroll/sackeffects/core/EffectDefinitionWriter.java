package com.sack.rpgroll.sackeffects.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class EffectDefinitionWriter {

    private final File folder;

    public EffectDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "effects");
    }

    public void save(EffectDefinition effect) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", effect.id());
        config.set("display-name", effect.displayName());
        config.set("description", effect.description());

        java.util.List<Map<String, Object>> steps = new java.util.ArrayList<>();

        for (EffectStep step : effect.steps()) {

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", step.type().name());
            map.put("delay", step.delayTicks());
            map.putAll(step.params());

            steps.add(map);
        }

        config.set("steps", steps);

        try {
            folder.mkdirs();
            config.save(new File(folder, effect.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el efecto " + effect.id(), e);
        }
    }

}
