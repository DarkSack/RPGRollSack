package com.sack.rpgroll.workers.core.worker;

/**
 * Cada worker tiene UN rasgo dominante que modula su IA — no una
 * combinación de varios, para mantener el modelo simple y predecible.
 */
public enum PersonalityTrait {
    LAZY,
    RESPONSIBLE,
    FAST,
    SMART,
    CLUMSY,
    AMBITIOUS,
    SOCIAL,
    LONER,
    BRAVE,
    COWARD;

    /** Multiplicador sobre la velocidad de trabajo (rompe bloques/cosecha/etc. más o menos rápido). */
    public double workSpeedMultiplier() {
        return switch (this) {
            case LAZY -> 0.7;
            case FAST -> 1.3;
            case RESPONSIBLE -> 1.1;
            case AMBITIOUS -> 1.15;
            default -> 1.0;
        };
    }

    /** Multiplicador sobre cuánto tarda en decaer el estrés — perezosos/sociales se estresan menos. */
    public double stressResistance() {
        return switch (this) {
            case LAZY, SOCIAL -> 0.7;
            case AMBITIOUS, COWARD -> 1.3;
            default -> 1.0;
        };
    }

    /** Multiplicador sobre la chance de cometer un error (perder calidad/objetos) al trabajar. */
    public double errorChanceMultiplier() {
        return switch (this) {
            case CLUMSY -> 2.0;
            case SMART, RESPONSIBLE -> 0.5;
            default -> 1.0;
        };
    }

}
