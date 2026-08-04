package com.sack.rpgroll.guilds.team;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Registro en memoria de equipos activos y sus invitaciones pendientes.
 * Un jugador está en a lo sumo un equipo a la vez. No persiste — los
 * equipos son temporales por diseño (spec: "grupos temporales").
 */
public class TeamManager {

    private static final long INVITE_EXPIRY_MILLIS = 60_000;

    private final Map<UUID, Team> teamByMember = new HashMap<>();
    private final Map<UUID, PendingInvite> invitesByTarget = new HashMap<>();

    private record PendingInvite(UUID teamId, UUID inviterId, long expiresAtMillis) {
    }

    public Optional<Team> getTeam(UUID playerId) {
        return Optional.ofNullable(teamByMember.get(playerId));
    }

    public boolean isInTeam(UUID playerId) {
        return teamByMember.containsKey(playerId);
    }

    /** Crea (si hace falta) el equipo del líder y registra una invitación pendiente para el destinatario. */
    public enum InviteResult {
        OK,
        ALREADY_TEAMMATE,
        TARGET_IN_TEAM,
        TEAM_FULL,
        NOT_ALLOWED
    }

    public InviteResult invite(Player inviter, Player target) {

        Team team = teamByMember.get(inviter.getUniqueId());

        if (team != null && !team.roleOf(inviter.getUniqueId()).canInvite()) {
            return InviteResult.NOT_ALLOWED;
        }

        if (team == null) {
            team = new Team(inviter.getUniqueId());
            teamByMember.put(inviter.getUniqueId(), team);
        }

        if (team.contains(target.getUniqueId())) {
            return InviteResult.ALREADY_TEAMMATE;
        }

        if (teamByMember.containsKey(target.getUniqueId())) {
            return InviteResult.TARGET_IN_TEAM;
        }

        if (team.size() >= team.maxPlayers()) {
            return InviteResult.TEAM_FULL;
        }

        invitesByTarget.put(target.getUniqueId(),
                new PendingInvite(team.id(), inviter.getUniqueId(), System.currentTimeMillis() + INVITE_EXPIRY_MILLIS));

        return InviteResult.OK;
    }

    public enum AcceptResult {
        OK,
        NO_INVITE,
        EXPIRED,
        TEAM_GONE,
        TEAM_FULL
    }

    public AcceptResult accept(Player target) {

        PendingInvite invite = invitesByTarget.remove(target.getUniqueId());

        if (invite == null) {
            return AcceptResult.NO_INVITE;
        }

        if (System.currentTimeMillis() > invite.expiresAtMillis()) {
            return AcceptResult.EXPIRED;
        }

        Team team = findTeamById(invite.teamId());

        if (team == null) {
            return AcceptResult.TEAM_GONE;
        }

        if (team.size() >= team.maxPlayers()) {
            return AcceptResult.TEAM_FULL;
        }

        team.addMember(target.getUniqueId(), TeamRole.MEMBER);
        teamByMember.put(target.getUniqueId(), team);

        return AcceptResult.OK;
    }

    public void decline(UUID targetId) {
        invitesByTarget.remove(targetId);
    }

    /** @return true si el equipo se disolvió por completo al irse este jugador. */
    public boolean leave(UUID playerId) {

        Team team = teamByMember.remove(playerId);

        if (team == null) {
            return false;
        }

        boolean disbanded = team.removeMember(playerId);

        if (disbanded) {
            return true;
        }

        for (UUID memberId : team.members()) {
            teamByMember.put(memberId, team);
        }

        return false;
    }

    public enum KickResult {
        OK,
        NOT_ALLOWED,
        NOT_IN_TEAM,
        CANNOT_KICK_SELF
    }

    public KickResult kick(Player actor, UUID targetId) {

        Team team = teamByMember.get(actor.getUniqueId());

        if (team == null || !team.contains(targetId)) {
            return KickResult.NOT_IN_TEAM;
        }

        if (targetId.equals(actor.getUniqueId())) {
            return KickResult.CANNOT_KICK_SELF;
        }

        TeamRole actorRole = team.roleOf(actor.getUniqueId());
        TeamRole targetRole = team.roleOf(targetId);

        if (!actorRole.canKick() || !actorRole.outranks(targetRole)) {
            return KickResult.NOT_ALLOWED;
        }

        teamByMember.remove(targetId);
        team.removeMember(targetId);

        return KickResult.OK;
    }

    public enum RoleChangeResult {
        OK,
        NOT_ALLOWED,
        NOT_IN_TEAM,
        TARGET_IS_LEADER
    }

    public RoleChangeResult setRole(Player actor, UUID targetId, TeamRole role) {

        Team team = teamByMember.get(actor.getUniqueId());

        if (team == null || !team.contains(targetId)) {
            return RoleChangeResult.NOT_IN_TEAM;
        }

        if (team.isLeader(targetId)) {
            return RoleChangeResult.TARGET_IS_LEADER;
        }

        if (!team.roleOf(actor.getUniqueId()).canEditConfig() && !team.isLeader(actor.getUniqueId())) {
            return RoleChangeResult.NOT_ALLOWED;
        }

        team.setRole(targetId, role);
        return RoleChangeResult.OK;
    }

    public void onQuit(UUID playerId) {
        leave(playerId);
        invitesByTarget.remove(playerId);
    }

    public Team findTeamById(UUID teamId) {
        return teamByMember.values().stream()
                .filter(team -> team.id().equals(teamId))
                .findFirst()
                .orElse(null);
    }

    /** Registra un equipo ya armado externamente (ej. por matchmaking) para que sus miembros queden indexados. */
    public void register(Team team) {
        for (UUID memberId : team.members()) {
            teamByMember.put(memberId, team);
        }
    }

}
