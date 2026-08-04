package com.sack.rpgroll.fishing.core;

public enum FishRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY;

    /** Peso base relativo en la tirada ponderada — a mayor rareza, muchísimo menos probable. */
    public double baseWeight() {
        return switch (this) {
            case COMMON -> 100.0;
            case UNCOMMON -> 40.0;
            case RARE -> 15.0;
            case EPIC -> 4.0;
            case LEGENDARY -> 0.5;
        };
    }

}
