package com.sack.rpgroll.ranching.core.nutrition;

public enum FeedQuality {
    COMMON,
    GOOD,
    PREMIUM,
    ORGANIC,
    LEGENDARY;

    /** Multiplicador sobre los bonos de nutrición/salud/felicidad/producción de este alimento. */
    public double multiplier() {
        return switch (this) {
            case COMMON -> 1.0;
            case GOOD -> 1.25;
            case PREMIUM -> 1.6;
            case ORGANIC -> 2.0;
            case LEGENDARY -> 3.0;
        };
    }

}
