package com.sack.rpgroll.fx.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

/**
 * Una secuencia de efectos reusable por id — partículas, sonidos, títulos,
 * action bar, boss bar y efectos de poción combinados en el orden que
 * quieras, cada uno con su propio delay desde que se dispara la secuencia.
 */
public record EffectDefinition(String id, String displayName, String description, List<EffectStep> steps,
        String icon) implements RPGContent {

    /** Sin icono propio: el navegador usa el de por defecto. */
    public EffectDefinition(String id, String displayName, String description, List<EffectStep> steps) {
        this(id, displayName, description, steps, null);
    }

    public EffectDefinition {
        Objects.requireNonNull(id, "id no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
        steps = steps == null ? List.of() : List.copyOf(steps);
        icon = icon == null || icon.isBlank() ? null : icon.trim();
    }

}
