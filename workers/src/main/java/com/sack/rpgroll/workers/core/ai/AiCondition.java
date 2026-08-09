package com.sack.rpgroll.workers.core.ai;

/**
 * Condiciones evaluables contra el estado de un worker + el mundo — la
 * "hoja" de un árbol de comportamiento real de verdad no es viable en
 * una GUI de Bukkit, así que se simula con una lista de reglas
 * condición→acción ordenadas por prioridad (ver {@link com.sack.rpgroll.workers.core.profession.AiRule}).
 */
public enum AiCondition {
    HUNGRY,
    TIRED,
    SLEEPY,
    STRESSED,
    INVENTORY_FULL,
    RAINING,
    NIGHT,
    LOW_HAPPINESS,
    HAS_TASK,
    ALWAYS
}
