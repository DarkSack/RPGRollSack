package com.sack.rpgroll.workers.core.schedule;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScheduleDefinitionWriter {

    private final File folder;

    public ScheduleDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "schedules");
    }

    public void save(Schedule schedule) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", schedule.id());
        config.set("display-name", schedule.displayName());
        config.set("description", schedule.description());

        List<Map<String, Object>> entries = new java.util.ArrayList<>();

        for (ScheduleEntry entry : schedule.entries()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("start-tick", entry.startTick());
            map.put("activity", entry.activity().name());
            entries.add(map);
        }

        config.set("entries", entries);

        try {
            folder.mkdirs();
            config.save(new File(folder, schedule.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el horario " + schedule.id(), e);
        }
    }

}
