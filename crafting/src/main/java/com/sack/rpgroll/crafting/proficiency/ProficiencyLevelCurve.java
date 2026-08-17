package com.sack.rpgroll.crafting.proficiency;

/**
 * Curva de nivel de proficiencia: {@code totalXpToReach(level)} crece
 * cuadráticamente, así cada nivel pide progresivamente más que el anterior
 * (mismo tipo de curva que un sistema de nivel de personaje típico).
 */
public final class ProficiencyLevelCurve {

    public static final int MAX_LEVEL = 50;
    private static final double BASE_XP = 50;

    private ProficiencyLevelCurve() {
    }

    /** Xp total acumulado necesario para HABER LLEGADO a este nivel (nivel 1 = 0). */
    public static double totalXpToReach(int level) {
        int clamped = Math.max(1, level);
        return BASE_XP * (clamped - 1) * clamped;
    }

    public static int levelFor(double totalXp) {

        int level = 1;
        while (level < MAX_LEVEL && totalXp >= totalXpToReach(level + 1)) {
            level++;
        }
        return level;
    }

    /** Progreso 0-1 dentro del nivel actual, para barras de progreso. */
    public static double progressWithinLevel(double totalXp) {

        int level = levelFor(totalXp);
        if (level >= MAX_LEVEL) {
            return 1.0;
        }

        double floor = totalXpToReach(level);
        double ceiling = totalXpToReach(level + 1);
        return Math.min(1, Math.max(0, (totalXp - floor) / (ceiling - floor)));
    }

    /** Factor 0-1 usado como {@code skillFactor} en el roll de calidad — nivel máximo = 1.0. */
    public static double factorFor(double totalXp) {
        return Math.min(1.0, levelFor(totalXp) / (double) MAX_LEVEL);
    }

}
