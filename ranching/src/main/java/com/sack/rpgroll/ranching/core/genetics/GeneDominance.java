package com.sack.rpgroll.ranching.core.genetics;

/**
 * Cómo se combinan los dos alelos de un animal para un gen en el valor de
 * fenotipo expresado (0-100). Un alelo es simplemente un número — no hay
 * un modelo discreto "A/a" de libro de texto, porque los genes de este
 * addon representan porcentajes continuos (producción de leche, fertilidad,
 * etc.), no rasgos binarios.
 */
public enum GeneDominance {

    /** El alelo más alto domina — un solo padre con una copia fuerte alcanza para expresarla. */
    DOMINANT,

    /** El alelo más bajo domina — hace falta que AMBAS copias sean fuertes para expresarse del todo. */
    RECESSIVE,

    /** Codominancia/mezcla — el fenotipo es el promedio de ambos alelos. */
    MIXED;

    public double resolve(double alleleA, double alleleB) {
        return switch (this) {
            case DOMINANT -> Math.max(alleleA, alleleB);
            case RECESSIVE -> Math.min(alleleA, alleleB);
            case MIXED -> (alleleA + alleleB) / 2.0;
        };
    }

}
