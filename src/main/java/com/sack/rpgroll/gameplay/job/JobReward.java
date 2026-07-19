package com.sack.rpgroll.gameplay.job;

/**
 * Recompensa otorgada por una acción específica dentro de un trabajo
 * (ej. romper un bloque de diamante siendo Minero).
 */
public record JobReward(double money, int experience) {

    public JobReward {
        if (money < 0) {
            throw new IllegalArgumentException("money no puede ser negativo");
        }
        if (experience < 0) {
            throw new IllegalArgumentException("experience no puede ser negativo");
        }
    }

}