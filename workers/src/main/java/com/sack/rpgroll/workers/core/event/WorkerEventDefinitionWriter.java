package com.sack.rpgroll.workers.core.event;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class WorkerEventDefinitionWriter {

    private final File folder;

    public WorkerEventDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "worker-events");
    }

    public void save(WorkerEventDefinition event) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", event.id());
        config.set("display-name", event.displayName());
        config.set("description", event.description());
        config.set("type", event.type().name());
        config.set("chance", event.chance());
        config.set("duration-ticks", event.durationTicks());
        config.set("happiness-delta", event.happinessDelta());
        config.set("energy-delta", event.energyDelta());
        config.set("health-delta", event.healthDelta());
        config.set("work-speed-multiplier-while-active", event.workSpeedMultiplierWhileActive());

        try {
            folder.mkdirs();
            config.save(new File(folder, event.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el evento " + event.id(), e);
        }
    }

}
