package com.sack.rpgroll.mobs.core;

import java.util.List;
import java.util.Map;

/**
 * Una fase de jefe: al cruzar {@code healthThresholdPercent} de vida hacia
 * abajo, el mob adopta los multiplicadores de stats, skills adicionales,
 * override de bossbar/diálogo y partícula de aura de esta fase.
 */
public record MobPhase(
        String id,
        double healthThresholdPercent,
        Map<String, Double> statMultipliers,
        List<MobSkill> skills,
        String bossBarColor,
        String bossBarTitle,
        String dialogueLine,
        String particleEffect) {

    public MobPhase {
        statMultipliers = statMultipliers == null ? Map.of() : Map.copyOf(statMultipliers);
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

}
