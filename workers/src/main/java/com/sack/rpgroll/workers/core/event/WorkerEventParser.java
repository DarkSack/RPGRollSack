package com.sack.rpgroll.workers.core.event;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;

public class WorkerEventParser implements ContentParser<WorkerEventDefinition> {

    @Override
    public WorkerEventDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new WorkerEventDefinition(
                id,
                config.getString("display-name", id),
                config.getString("description", ""),
                parseType(config.getString("type")),
                config.getDouble("chance", 0),
                config.getLong("duration-ticks", 6000L),
                config.getDouble("happiness-delta", 0),
                config.getDouble("energy-delta", 0),
                config.getDouble("health-delta", 0),
                config.getDouble("work-speed-multiplier-while-active", 1.0));
    }

    private WorkerEventType parseType(String raw) {

        if (raw == null || raw.isBlank()) {
            return WorkerEventType.ILLNESS;
        }

        try {
            return WorkerEventType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return WorkerEventType.ILLNESS;
        }
    }

}
