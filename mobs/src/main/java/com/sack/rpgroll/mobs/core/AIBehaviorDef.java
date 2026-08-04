package com.sack.rpgroll.mobs.core;

import java.util.List;

/**
 * Comportamiento de movimiento/combate del mob. {@code goals} se evalúan
 * EN ORDEN cada tick de IA (ver {@code MobAITask}) — el primero que
 * aplique gana ese tick. Valores válidos: ATTACK_PLAYERS, FLEE, PATROL,
 * GUARD_REGION, DEFEND_ALLY, FOLLOW_LEADER, STAY_STILL.
 */
public record AIBehaviorDef(
        List<String> goals,
        double aggroRange,
        double fleeHealthPercent,
        double moveSpeed,
        List<String> patrolPoints,
        String guardRegionId) {

    public AIBehaviorDef {
        goals = goals == null || goals.isEmpty() ? List.of("ATTACK_PLAYERS") : List.copyOf(goals);
        patrolPoints = patrolPoints == null ? List.of() : List.copyOf(patrolPoints);
        aggroRange = aggroRange <= 0 ? 16.0 : aggroRange;
        moveSpeed = moveSpeed <= 0 ? 1.0 : moveSpeed;
    }

}
