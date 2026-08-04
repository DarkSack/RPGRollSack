package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Logro de PERSONAJE (no global del jugador) — se otorga manualmente vía
 * comando o API por ahora, no hay detección automática todavía.
 * {@code description} sirve como recordatorio de qué bono conceptual
 * representa (ej. "+3% daño con espadas"); aplicar ese bono en el motor
 * de combate queda para una próxima iteración.
 */
public record Achievement(String id, String displayName, String description) implements RPGContent {

    public Achievement {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        description = description == null ? "" : description;
    }

}
