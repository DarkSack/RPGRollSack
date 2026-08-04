package com.sack.rpgroll.magic.core;

import java.util.Map;
import java.util.Objects;

/**
 * Un paso dentro del pipeline de un {@link Spell} — mismo patrón "tipo +
 * params libres" que {@code EffectComponent}/{@code ItemAction} en el resto
 * del proyecto.
 */
public record SpellComponent(SpellComponentType type, Map<String, String> params) {

    public SpellComponent {
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

    public boolean paramBoolean(String key, boolean fallback) {
        return params.containsKey(key) ? Boolean.parseBoolean(params.get(key)) : fallback;
    }

}
