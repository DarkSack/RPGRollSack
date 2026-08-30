package com.sack.rpgroll.guilds.team;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamManagerTest {

    private TeamManager manager;
    private Player leader;
    private Player invitee;

    private Player playerWithId(UUID id) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        return player;
    }

    @BeforeEach
    void setUp() {
        manager = new TeamManager();
        leader = playerWithId(UUID.randomUUID());
        invitee = playerWithId(UUID.randomUUID());
    }

    @Test
    void inviteCreatesTeamAndRegistersPendingInvite() {
        TeamManager.InviteResult result = manager.invite(leader, invitee);

        assertEquals(TeamManager.InviteResult.OK, result);
        assertTrue(manager.isInTeam(leader.getUniqueId()));
        assertFalse(manager.isInTeam(invitee.getUniqueId()));
    }

    @Test
    void inviteFailsWhenTargetAlreadyOnATeam() {
        Player third = playerWithId(UUID.randomUUID());
        manager.invite(leader, invitee);
        manager.accept(invitee);

        TeamManager.InviteResult result = manager.invite(third, invitee);

        assertEquals(TeamManager.InviteResult.TARGET_IN_TEAM, result);
    }

    @Test
    void inviteFailsWhenAlreadyTeammates() {
        manager.invite(leader, invitee);
        manager.accept(invitee);

        assertEquals(TeamManager.InviteResult.ALREADY_TEAMMATE, manager.invite(leader, invitee));
    }

    @Test
    void inviteFailsWhenTeamIsFull() {
        manager.invite(leader, invitee);
        Team team = manager.getTeam(leader.getUniqueId()).orElseThrow();
        team.setMaxPlayers(1);

        Player another = playerWithId(UUID.randomUUID());
        assertEquals(TeamManager.InviteResult.TEAM_FULL, manager.invite(leader, another));
    }

    @Test
    void memberWithoutInvitePermissionCannotInvite() {
        manager.invite(leader, invitee);
        manager.accept(invitee);

        Player another = playerWithId(UUID.randomUUID());
        assertEquals(TeamManager.InviteResult.NOT_ALLOWED, manager.invite(invitee, another));
    }

    @Test
    void acceptWithoutPendingInviteFails() {
        assertEquals(TeamManager.AcceptResult.NO_INVITE, manager.accept(invitee));
    }

    @Test
    void acceptAddsTargetToTeam() {
        manager.invite(leader, invitee);

        assertEquals(TeamManager.AcceptResult.OK, manager.accept(invitee));
        assertTrue(manager.isInTeam(invitee.getUniqueId()));
        assertEquals(2, manager.getTeam(leader.getUniqueId()).orElseThrow().size());
    }

    @Test
    void declineRemovesPendingInviteSoAcceptFailsAfterwards() {
        manager.invite(leader, invitee);
        manager.decline(invitee.getUniqueId());

        assertEquals(TeamManager.AcceptResult.NO_INVITE, manager.accept(invitee));
    }

    @Test
    void leaveDisbandsSoleSurvivorTeam() {
        manager.invite(leader, invitee);

        assertTrue(manager.leave(leader.getUniqueId()));
    }

    @Test
    void leaveKeepsTeamAliveForRemainingMembers() {
        manager.invite(leader, invitee);
        manager.accept(invitee);

        boolean disbanded = manager.leave(leader.getUniqueId());

        assertFalse(disbanded);
        assertTrue(manager.isInTeam(invitee.getUniqueId()));
    }

    @Test
    void leaveOfPlayerNotInAnyTeamReturnsFalse() {
        assertFalse(manager.leave(UUID.randomUUID()));
    }

    @Test
    void kickFailsWhenActorCannotOutrankTarget() {
        manager.invite(leader, invitee);
        manager.accept(invitee);

        TeamManager.KickResult result = manager.kick(invitee, leader.getUniqueId());
        assertEquals(TeamManager.KickResult.NOT_ALLOWED, result);
    }

    @Test
    void kickRemovesTargetFromTeam() {
        manager.invite(leader, invitee);
        manager.accept(invitee);

        assertEquals(TeamManager.KickResult.OK, manager.kick(leader, invitee.getUniqueId()));
        assertFalse(manager.isInTeam(invitee.getUniqueId()));
    }

    @Test
    void kickCannotTargetSelf() {
        manager.invite(leader, invitee);
        assertEquals(TeamManager.KickResult.CANNOT_KICK_SELF, manager.kick(leader, leader.getUniqueId()));
    }

    @Test
    void onQuitRemovesPlayerFromTeamSoTheirPendingInviteNoLongerResolves() {
        manager.invite(leader, invitee);

        manager.onQuit(leader.getUniqueId());

        assertFalse(manager.isInTeam(leader.getUniqueId()));
        assertEquals(TeamManager.AcceptResult.TEAM_GONE, manager.accept(invitee));
    }

    @Test
    void onQuitClearsAnyPendingInviteAddressedToTheQuittingPlayer() {
        manager.invite(leader, invitee);

        manager.onQuit(invitee.getUniqueId());

        assertEquals(TeamManager.AcceptResult.NO_INVITE, manager.accept(invitee));
    }
}
