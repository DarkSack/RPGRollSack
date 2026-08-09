package com.sack.rpgroll.economy.tax;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Objects;

/**
 * Una regla tributaria configurable. El motor ({@link TaxEngine}) aplica
 * TODAS las reglas cuyo {@code type} coincida con la operación — el
 * resultado final es un "money sink": el dinero retenido no se deposita en
 * ningún lado, simplemente se retira de circulación (ver Novedades/Roadmap
 * de money sinks en la documentación).
 */
public record TaxRule(
        String id,
        String displayName,
        TaxType type,
        double ratePercent,
        List<String> appliesTo,
        boolean enabled) implements RPGContent {

    public TaxRule {
        Objects.requireNonNull(id, "id no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        type = type == null ? TaxType.SALE : type;
        ratePercent = Math.max(0, Math.min(100, ratePercent));
        appliesTo = appliesTo == null ? List.of() : List.copyOf(appliesTo);
    }

    /** @return true si esta regla aplica a la categoría/id de producto dado (lista vacía = aplica a todo). */
    public boolean matches(String categoryOrId) {
        return appliesTo.isEmpty() || appliesTo.contains(categoryOrId);
    }

}
