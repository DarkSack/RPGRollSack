package com.sack.rpgroll.effects.core;

import java.util.Map;
import java.util.Objects;

/**
 * Un componente dentro de una {@link EffectDefinition} — mismo patrón "tipo
 * + params libres" que {@code ItemAction}/{@code MobAction}/{@code
 * EffectStep} de SackEffects, más un {@link EffectTriggerType} propio (cada
 * componente decide en qué momento dispara, no el efecto entero).
 */
public record EffectComponent(EffectComponentType type, EffectTriggerType trigger, Map<String, String> params) {

    public EffectComponent {
        Objects.requireNonNull(type, "type no puede ser null");
        trigger = trigger == null ? EffectTriggerType.ON_APPLY : trigger;
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

    public boolean paramBoolean(String key, boolean fallback) {
        return params.containsKey(key) ? Boolean.parseBoolean(params.get(key)) : fallback;
    }

}
