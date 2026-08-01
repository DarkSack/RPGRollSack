package com.sack.rpgroll.player.progression;

/**
 * Información de progresión de un jugador.
 *
 * Record inmutable que representa:
 * - Nivel actual
 * - Experiencia acumulada
 * - Timestamps
 * - Puntos de estadística ganados por nivel y aún no gastados
 */
public record PlayerProgression(
        int level,
        int experience,
        long createdAt,
        long lastLogin,
        int unspentStatPoints
) {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 100;
    public static final int BASE_EXP = 100;
    public static final double EXP_MULTIPLIER = 1.5;

    public PlayerProgression {
        if (unspentStatPoints < 0) {
            throw new IllegalArgumentException("unspentStatPoints no puede ser negativo");
        }
    }

    /**
     * Factory method para crear progresión inicial.
     */
    public static PlayerProgression createNew() {
        long now = System.currentTimeMillis();
        return new PlayerProgression(1, 0, now, now, 0);
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

    /**
     * Devuelve una copia con puntos de estadística adicionales sin gastar.
     */
    public PlayerProgression addStatPoints(int amount) {
        return new PlayerProgression(level, experience, createdAt, lastLogin, unspentStatPoints + amount);
    }

    /**
     * Devuelve una copia con puntos de estadística gastados.
     *
     * @throws IllegalArgumentException si amount es mayor a los puntos disponibles
     */
    public PlayerProgression spendStatPoints(int amount) {
        if (amount > unspentStatPoints) {
            throw new IllegalArgumentException("No hay suficientes puntos de estadística disponibles");
        }
        return new PlayerProgression(level, experience, createdAt, lastLogin, unspentStatPoints - amount);
    }

    /**
     * Reemplaza directamente el total de puntos sin gastar. Pensado para
     * herramientas de administración (ej. reset de stats), no para el
     * flujo normal de juego.
     */
    public PlayerProgression withUnspentStatPoints(int amount) {
        return new PlayerProgression(level, experience, createdAt, lastLogin, amount);
    }

}
