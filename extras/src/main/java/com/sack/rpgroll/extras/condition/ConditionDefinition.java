package com.sack.rpgroll.extras.condition;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.extras.action.ExtrasAction;

import java.util.List;

/**
 * Un estado custom (sangrado, envenenado, maldito, congelado...) —
 * sección 15/16. {@code durationTicks} negativo = indefinido (hasta que
 * algo lo remueva explícitamente vía API/comando). {@code potionEffects}
 * son strings {@code "TYPE"} o {@code "TYPE:AMPLIFICADOR"}, reaplicados
 * cada {@code intervalTicks}.
 */
public record ConditionDefinition(
        String id,
        int durationTicks,
        double periodicDamage,
        int intervalTicks,
        List<String> potionEffects,
        List<ExtrasAction> onApply,
        List<ExtrasAction> onTick,
        List<ExtrasAction> onExpire) implements RPGContent {
}
