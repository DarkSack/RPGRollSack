package com.sack.rpgroll.items.core;

import java.util.List;

/**
 * Habilidad activa o pasiva de un ítem. Una habilidad ACTIVA se ejecuta al
 * dispararse {@code trigger} (con cooldown propio); una PASIVA aplica sus
 * efectos de forma continua mientras el ítem está equipado (motor de stats
 * la trata como un efecto permanente, no necesita trigger).
 */
public record ItemAbility(
        String id,
        String displayName,
        boolean passive,
        ItemTrigger trigger,
        long cooldownMillis,
        List<String> conditions,
        List<ItemAction> actions) {

    public ItemAbility {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

}
