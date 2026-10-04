package com.sack.rpgroll.crates.lucky;

import java.util.List;
import java.util.Objects;

/**
 * Lo que puede pasar al romper un lucky block. Sale con probabilidad
 * {@code weight} entre la suma de los weights de su bloque.
 *
 * @param luck     si es buena o mala suerte: decide el aviso y el sonido
 * @param announce si se anuncia a todo el servidor
 */
public record LuckyOutcome(String id, double weight, Luck luck, String message, boolean announce,
        List<LuckyAction> actions) {

    public enum Luck { GOOD, NEUTRAL, BAD }

    public LuckyOutcome {
        Objects.requireNonNull(id, "id");
        if (weight <= 0) {
            throw new IllegalArgumentException("el resultado '" + id + "' necesita weight mayor que 0");
        }
        luck = luck == null ? Luck.NEUTRAL : luck;
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

}
