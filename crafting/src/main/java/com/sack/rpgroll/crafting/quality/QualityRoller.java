package com.sack.rpgroll.crafting.quality;

import java.util.Random;

/**
 * Convierte una habilidad del jugador (0.0-1.0, típicamente
 * {@code nivel de job / nivel máximo esperado para esa receta}) en un
 * puntaje de calidad 0-100: 70% depende de la habilidad, 30% es azar, para
 * que subir de nivel mejore el promedio sin eliminar la sorpresa.
 */
public class QualityRoller {

    private static final double SKILL_WEIGHT = 0.7;
    private static final double RANDOM_WEIGHT = 0.3;

    private final Random random;

    public QualityRoller() {
        this(new Random());
    }

    public QualityRoller(Random random) {
        this.random = random;
    }

    public CraftQuality roll(double skillFactor) {

        double clampedSkill = Math.max(0, Math.min(1, skillFactor));
        double score = (clampedSkill * SKILL_WEIGHT + random.nextDouble() * RANDOM_WEIGHT) * 100;

        return CraftQuality.fromScore(score);
    }

}
