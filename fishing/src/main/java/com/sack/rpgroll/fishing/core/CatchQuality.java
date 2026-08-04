package com.sack.rpgroll.fishing.core;

/**
 * Calidad de UNA captura puntual — independiente de {@link FishRarity},
 * que es una propiedad fija de la especie. El mismo Salmón puede salir
 * COMMON o MASTERWORK según equipo/carnada/clima/hora.
 */
public enum CatchQuality {
    COMMON,
    GOOD,
    EXCELLENT,
    PERFECT,
    MASTERWORK;

    public double priceMultiplier() {
        return switch (this) {
            case COMMON -> 1.0;
            case GOOD -> 1.3;
            case EXCELLENT -> 1.7;
            case PERFECT -> 2.2;
            case MASTERWORK -> 3.0;
        };
    }

}
