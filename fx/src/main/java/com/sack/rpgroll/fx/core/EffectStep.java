package com.sack.rpgroll.fx.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Un paso dentro de una {@link EffectDefinition} — mismo patrón "tipo +
 * params libres" que {@code ItemAction}/{@code MobAction}/{@code
 * DungeonAction} en el resto del proyecto, más un {@code delayTicks}
 * (offset desde que se disparó la secuencia, no desde el paso anterior).
 */
public record EffectStep(EffectStepType type, int delayTicks, Map<String, String> params) {

    public EffectStep {
        Objects.requireNonNull(type, "type no puede ser null");
        delayTicks = Math.max(0, delayTicks);
        // Conserva el orden del YAML: Map.copyOf lo barajaba, y el editor mostraba y volvía a
        // guardar los parámetros de cada paso en un orden aleatorio.
        params = params == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(params));
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

    public EffectTarget paramTarget(String key, EffectTarget fallback) {
        try {
            return params.containsKey(key)
                    ? EffectTarget.valueOf(params.get(key).trim().toUpperCase(Locale.ROOT))
                    : fallback;
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

}
