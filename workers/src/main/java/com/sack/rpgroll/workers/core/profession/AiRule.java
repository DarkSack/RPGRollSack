package com.sack.rpgroll.workers.core.profession;

import com.sack.rpgroll.workers.core.ai.AiAction;
import com.sack.rpgroll.workers.core.ai.AiCondition;

import java.util.Objects;

/**
 * Una regla condición→acción dentro de una {@link Profession} — vive
 * DENTRO de la definición de la profesión (no es un tipo de contenido
 * propio con su navegador), mismo criterio que {@code GeneMutation}
 * dentro de {@code Gene} en RPGRoll-Ranching.
 *
 * @param priority menor se evalúa primero — la primera regla cuya condición matchea gana
 */
public record AiRule(AiCondition condition, AiAction action, int priority) {

    public AiRule {
        Objects.requireNonNull(condition, "condition no puede ser null");
        Objects.requireNonNull(action, "action no puede ser null");
    }

}
