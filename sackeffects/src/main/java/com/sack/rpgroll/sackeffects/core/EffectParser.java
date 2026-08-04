package com.sack.rpgroll.sackeffects.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EffectParser implements ContentParser<EffectDefinition> {

    @Override
    public EffectDefinition parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String description = config.getString("description", "");

        List<EffectStep> steps = parseSteps(config.getMapList("steps"), id);

        return new EffectDefinition(id, displayName, description, steps);
    }

    private List<EffectStep> parseSteps(List<?> rawSteps, String effectId) {

        List<EffectStep> steps = new ArrayList<>();

        for (Object rawStep : rawSteps) {

            if (!(rawStep instanceof Map<?, ?> map)) {
                continue;
            }

            Object rawType = map.get("type");
            if (rawType == null) {
                continue;
            }

            EffectStepType type;

            try {
                type = EffectStepType.valueOf(rawType.toString().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "efecto '" + effectId + "' tiene un step con type inválido: " + rawType);
            }

            int delay = 0;
            Object rawDelay = map.get("delay");
            if (rawDelay instanceof Number number) {
                delay = number.intValue();
            } else if (rawDelay != null) {
                try {
                    delay = Integer.parseInt(rawDelay.toString().trim());
                } catch (NumberFormatException ignored) {
                }
            }

            Map<String, String> params = new LinkedHashMap<>();

            for (var entry : map.entrySet()) {

                String key = String.valueOf(entry.getKey());

                if (key.equals("type") || key.equals("delay") || entry.getValue() == null) {
                    continue;
                }

                params.put(key, String.valueOf(entry.getValue()));
            }

            steps.add(new EffectStep(type, delay, params));
        }

        return steps;
    }

}
