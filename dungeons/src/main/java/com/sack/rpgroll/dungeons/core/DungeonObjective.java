package com.sack.rpgroll.dungeons.core;

import java.util.Map;

/**
 * Un objetivo de sala o de dungeon. {@code amount} es la cantidad
 * necesaria (kills, bloques, segundos...); {@code params} guarda datos
 * específicos del tipo (ej. {@code entity: ZOMBIE} para KILL_ENTITY,
 * {@code material: DIAMOND} para COLLECT_ITEM).
 */
public record DungeonObjective(DungeonObjectiveType type, String description, int amount,
        Map<String, String> params) {

    public DungeonObjective {
        amount = Math.max(1, amount);
        params = params == null ? Map.of() : Map.copyOf(params);
        description = description == null || description.isBlank() ? type.name() : description;
    }

    public String param(String key, String fallback) {
        return params.getOrDefault(key, fallback);
    }

}
