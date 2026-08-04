package com.sack.rpgroll.dungeons.core;

/**
 * KILL_ENTITY, COLLECT_ITEM, BREAK_BLOCK y SURVIVE_TIME progresan solos
 * (ver {@code DungeonObjectiveListener}/{@code DungeonSessionTask}). El
 * resto (ESCORT, ACTIVATE_MECHANISM, DEFEND, ESCAPE...) se progresan a
 * mano desde cualquier trigger con la acción {@code PROGRESS_OBJECTIVE}
 * — no tienen mecánica automática propia, es la forma en que este motor
 * cubre objetivos "narrativos" sin necesitar código nuevo por cada uno.
 */
public enum DungeonObjectiveType {
    SURVIVE_TIME,
    DEFEND,
    ESCORT,
    SOLVE_PUZZLE,
    KILL_ENTITY,
    DESTROY_BLOCK,
    COLLECT_ITEM,
    ACTIVATE_MECHANISM,
    ESCAPE,
    RESIST_TIME
}
