package com.sack.rpgroll.guilds.team.matchmaking;

import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamManager;
import com.sack.rpgroll.guilds.team.TeamRole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cola de emparejamiento automático. Agrupa solicitudes compatibles
 * (mismo dungeon/evento o "cualquiera", nivel dentro de tolerancia) en
 * cuanto se alcanza un mínimo de 2 jugadores, hasta el tamaño deseado.
 */
public class TeamMatchmakingQueue {

    private static final int MIN_GROUP_SIZE = 2;

    private final TeamManager teamManager;
    private final Map<UUID, MatchmakingRequest> pending = new LinkedHashMap<>();

    public TeamMatchmakingQueue(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void enqueue(MatchmakingRequest request) {
        pending.put(request.playerId(), request);
    }

    public void cancel(UUID playerId) {
        pending.remove(playerId);
    }

    public boolean isQueued(UUID playerId) {
        return pending.containsKey(playerId);
    }

    /** @return los equipos recién formados en esta pasada (ya registrados en el TeamManager). */
    public List<Team> tryMatch() {

        List<Team> formed = new ArrayList<>();
        Map<String, List<MatchmakingRequest>> byBucket = new LinkedHashMap<>();

        for (MatchmakingRequest request : pending.values()) {
            byBucket.computeIfAbsent(request.bucketKey(), k -> new ArrayList<>()).add(request);
        }

        for (List<MatchmakingRequest> bucket : byBucket.values()) {

            bucket.sort((a, b) -> Long.compare(a.queuedAtMillis(), b.queuedAtMillis()));

            while (bucket.size() >= MIN_GROUP_SIZE) {

                MatchmakingRequest seed = bucket.get(0);
                int targetSize = Math.max(MIN_GROUP_SIZE, seed.desiredTeamSize());

                List<MatchmakingRequest> group = new ArrayList<>();
                group.add(seed);

                for (int i = 1; i < bucket.size() && group.size() < targetSize; i++) {
                    MatchmakingRequest candidate = bucket.get(i);
                    if (seed.compatibleWith(candidate)) {
                        group.add(candidate);
                    }
                }

                if (group.size() < MIN_GROUP_SIZE) {
                    bucket.remove(seed);
                    continue;
                }

                bucket.removeAll(group);
                group.forEach(request -> pending.remove(request.playerId()));

                Team team = new Team(group.get(0).playerId());
                team.setMaxPlayers(Math.max(targetSize, team.maxPlayers()));

                if (seed.dungeonId() != null) {
                    team.setLinkedDungeonId(seed.dungeonId());
                }
                if (seed.eventId() != null) {
                    team.setLinkedEventId(seed.eventId());
                }

                for (int i = 1; i < group.size(); i++) {
                    team.addMember(group.get(i).playerId(), TeamRole.MEMBER);
                }

                teamManager.register(team);
                formed.add(team);
            }
        }

        return formed;
    }

}
