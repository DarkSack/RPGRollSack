package com.sack.rpgroll.ranching.core.production;

/** Calidad de UNA unidad de producción puntual (una ordeñada, una esquilada...) — independiente de la calidad general del animal. */
public enum ProductQuality {
    COMMON,
    GOOD,
    PREMIUM,
    ORGANIC,
    LEGENDARY;

    public static ProductQuality fromScore(double score) {

        if (score >= 90) {
            return LEGENDARY;
        }

        if (score >= 75) {
            return ORGANIC;
        }

        if (score >= 55) {
            return PREMIUM;
        }

        if (score >= 30) {
            return GOOD;
        }

        return COMMON;
    }

    public double valueMultiplier() {
        return switch (this) {
            case COMMON -> 1.0;
            case GOOD -> 1.3;
            case PREMIUM -> 1.7;
            case ORGANIC -> 2.2;
            case LEGENDARY -> 3.5;
        };
    }

}
