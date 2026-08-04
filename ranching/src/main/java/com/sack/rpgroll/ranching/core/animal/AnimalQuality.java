package com.sack.rpgroll.ranching.core.animal;

/** Calidad general de UN animal puntual — se recalcula a partir de sus genes expresados y su bienestar acumulado. */
public enum AnimalQuality {
    POOR,
    COMMON,
    GOOD,
    EXCELLENT,
    EXCEPTIONAL;

    /** @param score 0-100, promedio ponderado de fenotipo genético y bienestar histórico. */
    public static AnimalQuality fromScore(double score) {

        if (score >= 90) {
            return EXCEPTIONAL;
        }

        if (score >= 75) {
            return EXCELLENT;
        }

        if (score >= 55) {
            return GOOD;
        }

        if (score >= 30) {
            return COMMON;
        }

        return POOR;
    }

    public double priceMultiplier() {
        return switch (this) {
            case POOR -> 0.5;
            case COMMON -> 1.0;
            case GOOD -> 1.5;
            case EXCELLENT -> 2.5;
            case EXCEPTIONAL -> 5.0;
        };
    }

}
