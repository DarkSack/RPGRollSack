package com.sack.rpgroll.fx.core;

/**
 * A quién/dónde se aplica un {@link EffectStep}, resuelto contra el
 * {@link com.sack.rpgroll.fx.engine.EffectContext} de cada disparo.
 */
public enum EffectTarget {
    /** Quien disparó el efecto. */
    SELF,
    /** La entidad/ubicación objetivo del contexto — si no hay, cae a SELF. */
    TARGET,
    /** Coordenadas absolutas leídas de los params del step (world/x/y/z). */
    LOCATION,
    /** Todos los jugadores dentro de un radio del punto de origen (params: radius). */
    ALL_NEARBY
}
