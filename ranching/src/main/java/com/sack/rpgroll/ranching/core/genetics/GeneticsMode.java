package com.sack.rpgroll.ranching.core.genetics;

/**
 * Nivel de complejidad de la herencia, elegido una vez en config.yml para
 * todo el servidor.
 */
public enum GeneticsMode {

    /** Promedio de ambos padres +/- una pequeña variación al azar — sin dominancia ni alelos separados. */
    SIMPLE,

    /** Herencia real por alelo (dominante/recesivo/mixto), un alelo al azar de cada padre. */
    ADVANCED,

    /** Igual que ADVANCED, pero antes de aparear se puede previsualizar la distribución completa de probabilidad. */
    PROBABILISTIC
}
