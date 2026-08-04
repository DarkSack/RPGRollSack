package com.sack.rpgroll.dungeons.core;

/**
 * Modificadores globales aplicados durante toda la corrida, atados a una
 * {@link DungeonDifficulty}. Set fijo — a diferencia de las acciones, no
 * son extensibles por registro (son efectos de estado, no eventos
 * puntuales).
 */
public enum DungeonModifierType {
    NO_HEALING,
    DOUBLE_DAMAGE_TAKEN,
    CONSTANT_RAIN,
    PERMANENT_DARKNESS,
    REDUCED_TIME_LIMIT
}
