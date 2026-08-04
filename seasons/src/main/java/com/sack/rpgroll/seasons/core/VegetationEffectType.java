package com.sack.rpgroll.seasons.core;

/** Reacciones de bloques/mundo que {@code VegetationTask} aplica cerca de jugadores mientras esta estación está activa. */
public enum VegetationEffectType {
    /** Capas de nieve sobre pasto/tierra en biomas fríos (temperatura &lt; 0°C). */
    SNOW_LAYERS,
    /** Congela la superficie de agua expuesta en biomas fríos. */
    ICE_LAKES,
    /** Reduce la humedad de tierra de cultivo sin regar cerca, simulando sequía. */
    DRY_GRASS,
    /** Partículas de hojas cayendo cerca de árboles. */
    FALLING_LEAVES,
    /** Florece pasto alto cercano en flores al azar. */
    FLOWER_BOOM
}
