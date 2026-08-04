package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Map;
import java.util.Objects;

/**
 * Modifica el comportamiento de un hechizo al que esté adjunta (vía {@code
 * PlayerSpellbook#attachedRunes}) — mismo patrón "tipo + params" que
 * {@code SpellComponent}, aplicado por {@code RuneModifierApplier} sobre
 * una copia de trabajo del pipeline en el momento del cast.
 */
public record Rune(String id, String displayName, String icon, String description, RuneModifierType type,
        Map<String, String> params) implements RPGContent {

    public Rune {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "EMERALD" : icon;
        description = description == null ? "" : description;
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
