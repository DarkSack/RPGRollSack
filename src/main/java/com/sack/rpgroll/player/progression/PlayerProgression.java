package com.sack.rpgroll.player.progression;

/**
 * Información de progresión de un jugador.
 * 
 * Record inmutable que representa:
 * - Nivel actual
 * - Experiencia acumulada
 * - Timestamps
 */
public record PlayerProgression(
        int level,
        int experience,
        long createdAt,
        long lastLogin
) {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 100;
    public static final int BASE_EXP = 100;
    public static final double EXP_MULTIPLIER = 1.5;

    /**
     * Factory method para crear progresión inicial.
     */
    public static PlayerProgression createNew() {
        long now = System.currentTimeMillis();
        return new PlayerProgression(1, 0, now, now);
    }

    /**
     * Calcula la experiencia necesaria para alcanzar el siguiente nivel.
     * Formula: 100 * (nivel ^ 1.5)
     */
    public int getRequiredExpForNextLevel() {
        if (level >= MAX_LEVEL) {
            return Integer.MAX_VALUE;
        }
        return (int) (BASE_EXP * Math.pow(level + 1, EXP_MULTIPLIER));
    }

    /**
     * Calcula la experiencia necesaria para alcanzar el nivel actual.
     * Formula: 100 * (nivel ^ 1.5)
     */
    public int getRequiredExpForCurrentLevel() {
        if (level <= MIN_LEVEL) {
            return 0;
        }
        return (int) (BASE_EXP * Math.pow(level, EXP_MULTIPLIER));
    }

    /**
     * Obtiene la experiencia necesaria hasta el siguiente nivel.
     */
    public int getExpToNextLevel() {
        int required = getRequiredExpForNextLevel();
        return Math.max(0, required - experience);
    }

    /**
     * Obtiene el progreso hacia el siguiente nivel como porcentaje (0-100).
     */
    public int getProgressPercent() {
        if (level >= MAX_LEVEL) {
            return 100;
        }

        int current = getRequiredExpForCurrentLevel();
        int next = getRequiredExpForNextLevel();
        int progress = experience - current;
        int required = next - current;

        return (int) ((progress * 100.0) / required);
    }

    /**
     * Verifica si ha alcanzado el nivel máximo.
     */
    public boolean isMaxLevel() {
        return level >= MAX_LEVEL;
    }

}
