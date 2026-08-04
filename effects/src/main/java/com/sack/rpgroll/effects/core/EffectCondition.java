package com.sack.rpgroll.effects.core;

import java.util.Map;
import java.util.Objects;

/** Un requisito dentro de una {@link EffectDefinition} — mismo patrón "tipo + params libres". */
public record EffectCondition(EffectConditionType type, Map<String, String> params) {

    public EffectCondition {
        Objects.requireNonNull(type, "type no puede ser null");
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public String param(String key, String fallback) {
        return params.getOrDefault(key, fallback);
    }

    public double paramDouble(String key, double fallback) {
        try {
            return params.containsKey(key) ? Double.parseDouble(params.get(key)) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public int paramInt(String key, int fallback) {
        try {
            return params.containsKey(key) ? Integer.parseInt(params.get(key)) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
