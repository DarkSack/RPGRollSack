package com.sack.rpgroll.gameplay.levelup;

import java.util.*;

/**
 * Recompensas otorgadas al subir de nivel.
 */
public record LevelUpRewards(
        int level,
        int experienceRequired,
        int statPoints,
        int healthBonus,
        int manaBonus,
        List<String> unlockedSkills,
        List<String> unlockedTraits) {

    /**
     * Crea recompensas vacías para un nivel.
     */
    public static LevelUpRewards empty(int level, int expRequired) {
        return new LevelUpRewards(level, expRequired, 0, 0, 0, List.of(), List.of());
    }

    /**
     * Verifica si hay recompensas en este level up.
     */
    public boolean hasRewards() {
        return statPoints > 0 || healthBonus > 0 || manaBonus > 0 ||
                !unlockedSkills.isEmpty() || !unlockedTraits.isEmpty();
    }

    /**
     * Obtiene un resumen formateado de las recompensas.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();

        if (statPoints > 0) {
            sb.append(statPoints).append(" Puntos de Estadística");
        }
        if (healthBonus > 0) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append("+").append(healthBonus).append(" Salud");
        }
        if (manaBonus > 0) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append("+").append(manaBonus).append(" Maná");
        }

        return sb.toString();
    }

}
