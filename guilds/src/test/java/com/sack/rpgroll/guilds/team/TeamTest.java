package com.sack.rpgroll.guilds.team;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamTest {

    private final UUID leader = UUID.randomUUID();
    private final UUID member = UUID.randomUUID();

    @Test
    void creatorIsLeaderAndSoleMember() {
        Team team = new Team(leader);

        assertTrue(team.isLeader(leader));
        assertEquals(1, team.size());
        assertEquals(TeamRole.LEADER, team.roleOf(leader));
    }

    @Test
    void unknownMemberDefaultsToGuestRole() {
        Team team = new Team(leader);
        assertEquals(TeamRole.GUEST, team.roleOf(UUID.randomUUID()));
    }

    @Test
    void removingLastMemberDisbandsTeam() {
        Team team = new Team(leader);
        assertTrue(team.removeMember(leader));
    }

    @Test
    void removingLeaderPromotesAnotherMember() {
        Team team = new Team(leader);
        team.addMember(member, TeamRole.MEMBER);

        boolean disbanded = team.removeMember(leader);

        assertFalse(disbanded);
        assertTrue(team.isLeader(member));
        assertEquals(TeamRole.LEADER, team.roleOf(member));
    }

    @Test
    void removingNonLeaderMemberKeepsTeamAlive() {
        Team team = new Team(leader);
        team.addMember(member, TeamRole.MEMBER);

        boolean disbanded = team.removeMember(member);

        assertFalse(disbanded);
        assertTrue(team.isLeader(leader));
    }

    @Test
    void setRoleCannotDemoteTheLeader() {
        Team team = new Team(leader);
        team.setRole(leader, TeamRole.GUEST);

        assertEquals(TeamRole.LEADER, team.roleOf(leader));
    }

    @Test
    void setRoleIgnoresPlayersNotInTeam() {
        Team team = new Team(leader);
        team.setRole(UUID.randomUUID(), TeamRole.OFFICER);
        assertEquals(1, team.size());
    }

    @Test
    void toggleBuffAddsThenRemoves() {
        Team team = new Team(leader);
        assertFalse(team.hasBuff(TeamBuff.XP_BOOST));

        team.toggleBuff(TeamBuff.XP_BOOST);
        assertTrue(team.hasBuff(TeamBuff.XP_BOOST));

        team.toggleBuff(TeamBuff.XP_BOOST);
        assertFalse(team.hasBuff(TeamBuff.XP_BOOST));
    }

    @Test
    void maxPlayersIsClampedToAtLeastOne() {
        Team team = new Team(leader);
        team.setMaxPlayers(-3);
        assertEquals(1, team.maxPlayers());
    }

    @Test
    void minLevelIsClampedToAtLeastZero() {
        Team team = new Team(leader);
        team.setMinLevel(-5);
        assertEquals(0, team.minLevel());
    }

    @Test
    void waypointLookupIsCaseInsensitive() {
        Team team = new Team(leader);
        team.addWaypoint(new TeamWaypoint("Camp", "world", 0, 0, 0, System.currentTimeMillis()));

        assertTrue(team.waypoints().containsKey("camp"));

        team.removeWaypoint("CAMP");
        assertFalse(team.waypoints().containsKey("camp"));
    }
}
