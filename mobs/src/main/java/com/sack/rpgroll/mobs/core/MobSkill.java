package com.sack.rpgroll.mobs.core;

import java.util.List;

/**
 * Una habilidad del mob (bola de fuego, dash, invocar esbirros, etc.).
 * {@code trigger} nulo significa pasiva (no se dispara sola; queda
 * disponible para que otro sistema —ej. una fase— la ejecute a demanda).
 */
public record MobSkill(
        String id,
        String displayName,
        MobTrigger trigger,
        long cooldownMillis,
        double chance,
        List<String> conditions,
        List<MobAction> actions) {

    public MobSkill {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        actions = actions == null ? List.of() : List.copyOf(actions);
        chance = chance <= 0 ? 100.0 : chance;
    }

}
