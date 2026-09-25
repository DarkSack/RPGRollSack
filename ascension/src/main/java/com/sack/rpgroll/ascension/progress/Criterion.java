package com.sack.rpgroll.ascension.progress;

import java.util.Objects;

/**
 * Una condición de un logro, o una fuente de reputación de una facción.
 *
 * @param target filtro con comodín sobre el evento (ver {@link Glob})
 * @param held   filtro con comodín sobre el ítem en la mano, o {@code null}
 * @param key    facción u oficio de los criterios de estado que lo necesitan
 * @param amount en un logro, cuántas veces (contador) o qué valor mínimo
 *               (estado); en una fuente de reputación, cuánta da cada vez
 *               (puede ser negativa)
 */
public record Criterion(TriggerType type, String target, String held, String key, int amount) {

    public Criterion {
        Objects.requireNonNull(type, "type no puede ser null");
    }

    public boolean matches(ProgressEvent event) {
        return event.type() == type
                && Glob.matches(target, event.target())
                && (Glob.isWildcard(held) || Glob.matches(held, event.held()));
    }

}
