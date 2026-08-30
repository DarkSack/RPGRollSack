package com.sack.rpgroll.guilds.buff;

import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildBuff;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamBuff;
import com.sack.rpgroll.guilds.team.TeamManager;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuffCalculatorTest {

    private final UUID playerId = UUID.randomUUID();

    @Test
    void xpMultiplierIsOneWithNoTeamOrGuild() {
        TeamManager teamManager = mock(TeamManager.class);
        GuildManager guildManager = mock(GuildManager.class);
        when(teamManager.getTeam(playerId)).thenReturn(Optional.empty());
        when(guildManager.findByMember(playerId)).thenReturn(Optional.empty());

        BuffCalculator calculator = new BuffCalculator(teamManager, guildManager);

        assertEquals(1.0, calculator.xpMultiplier(playerId));
        assertEquals(1.0, calculator.lootMultiplier(playerId));
        assertEquals(0.0, calculator.damageDealtBonus(playerId));
    }

    @Test
    void xpMultiplierStacksTeamAndGuildBonusesAdditively() {
        TeamManager teamManager = mock(TeamManager.class);
        GuildManager guildManager = mock(GuildManager.class);

        Team team = new Team(playerId);
        team.toggleBuff(TeamBuff.XP_BOOST);
        when(teamManager.getTeam(playerId)).thenReturn(Optional.of(team));

        Guild guild = new Guild("crypt", "Crypt", playerId);
        guild.toggleBuff(GuildBuff.XP_BOOST);
        when(guildManager.findByMember(playerId)).thenReturn(Optional.of(guild));

        BuffCalculator calculator = new BuffCalculator(teamManager, guildManager);

        double expected = 1 + (TeamBuff.XP_BOOST.percent() + GuildBuff.XP_BOOST.percent());
        assertEquals(expected, calculator.xpMultiplier(playerId), 0.0001);
    }

    @Test
    void lootMultiplierOnlyConsidersGuildBuff() {
        TeamManager teamManager = mock(TeamManager.class);
        GuildManager guildManager = mock(GuildManager.class);

        Guild guild = new Guild("crypt", "Crypt", playerId);
        guild.toggleBuff(GuildBuff.LOOT_BOOST);
        when(guildManager.findByMember(playerId)).thenReturn(Optional.of(guild));

        BuffCalculator calculator = new BuffCalculator(teamManager, guildManager);

        assertEquals(1 + GuildBuff.LOOT_BOOST.percent(), calculator.lootMultiplier(playerId));
    }

    @Test
    void speedAndRegenBonusOnlyConsiderTeamBuffs() {
        TeamManager teamManager = mock(TeamManager.class);
        GuildManager guildManager = mock(GuildManager.class);
        when(guildManager.findByMember(playerId)).thenReturn(Optional.empty());

        Team team = new Team(playerId);
        team.toggleBuff(TeamBuff.SPEED_BOOST);
        team.toggleBuff(TeamBuff.REGEN_BOOST);
        when(teamManager.getTeam(playerId)).thenReturn(Optional.of(team));

        BuffCalculator calculator = new BuffCalculator(teamManager, guildManager);

        assertEquals(TeamBuff.SPEED_BOOST.percent(), calculator.speedBonus(playerId));
        assertEquals(TeamBuff.REGEN_BOOST.percent(), calculator.regenBonus(playerId));
    }

    @Test
    void damageTakenReductionOnlyConsidersGuildDefenseBuff() {
        TeamManager teamManager = mock(TeamManager.class);
        GuildManager guildManager = mock(GuildManager.class);
        when(guildManager.findByMember(playerId)).thenReturn(Optional.empty());

        BuffCalculator calculator = new BuffCalculator(teamManager, guildManager);

        assertEquals(0.0, calculator.damageTakenReduction(playerId));
    }
}
