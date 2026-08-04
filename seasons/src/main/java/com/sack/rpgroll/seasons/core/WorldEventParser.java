package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WorldEventParser implements ContentParser<WorldEvent> {

    @Override
    public WorldEvent parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new WorldEvent(id, config.getString("display-name", id), config.getString("icon", "NETHER_STAR"),
                config.getString("description", ""), config.getInt("duration", 200),
                parseComponents(config.getMapList("components"), id));
    }

    private List<WorldEventComponent> parseComponents(List<?> raw, String eventId) {

        List<WorldEventComponent> components = new ArrayList<>();

        for (Object rawComponent : raw) {

            if (!(rawComponent instanceof Map<?, ?> map)) {
                continue;
            }

            Object rawType = map.get("type");
            if (rawType == null) {
                continue;
            }

            WorldEventComponentType type;

            try {
                type = WorldEventComponentType.valueOf(rawType.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "evento '" + eventId + "' tiene un componente con type inválido: " + rawType);
            }

            Map<String, String> params = new LinkedHashMap<>();

            for (var entry : map.entrySet()) {

                String key = String.valueOf(entry.getKey());

                if (key.equals("type") || entry.getValue() == null) {
                    continue;
                }

                params.put(key, String.valueOf(entry.getValue()));
            }

            components.add(new WorldEventComponent(type, params));
        }

        return components;
    }

}
