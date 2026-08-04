package com.sack.rpgroll.seasons.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorldEventDefinitionWriter {

    private final File folder;

    public WorldEventDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "events");
    }

    public void save(WorldEvent event) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", event.id());
        config.set("display-name", event.displayName());
        config.set("icon", event.icon());
        config.set("description", event.description());
        config.set("duration", event.durationTicks());

        List<Map<String, Object>> components = new java.util.ArrayList<>();

        for (WorldEventComponent component : event.components()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", component.type().name());
            map.putAll(component.params());
            components.add(map);
        }

        config.set("components", components);

        try {
            folder.mkdirs();
            config.save(new File(folder, event.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el evento " + event.id(), e);
        }
    }

}
