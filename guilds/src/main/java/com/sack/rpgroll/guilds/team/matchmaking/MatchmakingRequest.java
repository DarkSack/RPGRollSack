package com.sack.rpgroll.guilds.team.matchmaking;

import java.util.UUID;

/**
 * Solicitud de emparejamiento automático (spec: "por nivel/clase/rol/dungeon/evento").
 * {@code desiredRole} es un rol de contenido libre (ej. "tank"/"healer"/"dps"),
 * distinto de {@link com.sack.rpgroll.guilds.team.TeamRole} (que es de
 * permisos dentro del equipo).
 */
public record MatchmakingRequest(UUID playerId, int playerLevel, String desiredClass, String desiredRole,
        String dungeonId, String eventId, int desiredTeamSize, long queuedAtMillis) {

    private static final int LEVEL_TOLERANCE = 5;

    public String bucketKey() {
        if (dungeonId != null && !dungeonId.isBlank()) {
            return "dungeon:" + dungeonId.toLowerCase(java.util.Locale.ROOT);
        }
        if (eventId != null && !eventId.isBlank()) {
            return "event:" + eventId.toLowerCase(java.util.Locale.ROOT);
        }
        return "any";
    }

    public boolean compatibleWith(MatchmakingRequest other) {
        return Math.abs(playerLevel - other.playerLevel) <= LEVEL_TOLERANCE;
    }

}
