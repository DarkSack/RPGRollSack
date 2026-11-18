package com.sack.rpgroll.guilds.integration;

import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.GuildRole;
import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamManager;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuildsPlaceholdersTest {

    private final UUID playerId = UUID.randomUUID();
    private final Player player = mock(Player.class);
    private final TeamManager teams = mock(TeamManager.class);
    private final GuildManager guilds = mock(GuildManager.class);
    private GuildsPlaceholders placeholders;

    @BeforeEach
    void setUp() {
        when(player.getUniqueId()).thenReturn(playerId);
        when(teams.getTeam(playerId)).thenReturn(Optional.empty());
        when(guilds.findByMember(playerId)).thenReturn(Optional.empty());
        placeholders = new GuildsPlaceholders(mock(Plugin.class), teams, guilds);
    }

    @Test
    void showsSharedTreasuryForEveryGuildMemberAndReflectsBalanceChanges() {
        Guild guild = new Guild("guild", "Los Aventureros", UUID.randomUUID());
        guild.addMember(playerId, GuildRole.RECRUIT);
        when(guilds.findByMember(playerId)).thenReturn(Optional.of(guild));
        guild.vault().restoreBalance(1250.75);

        assertEquals("si", value("in_guild"));
        assertEquals("Los Aventureros", value("guild_name"));
        assertEquals("RECRUIT", value("guild_role"));
        assertEquals("2", value("guild_members"));
        assertEquals("1250.75", value("guild_balance"));
        Player leader = mock(Player.class);
        when(leader.getUniqueId()).thenReturn(guild.founderId());
        when(guilds.findByMember(guild.founderId())).thenReturn(Optional.of(guild));
        assertEquals("1250.75", placeholders.onPlaceholderRequest(leader, "guild_balance"));

        guild.vault().restoreBalance(400);
        assertEquals("400", value("guild_balance"));
    }

    @Test
    void resolvesOnlyTheRequestingPlayersGuild() {
        Guild ownGuild = new Guild("own", "Propio", playerId);
        ownGuild.vault().restoreBalance(10.5);
        Guild otherGuild = new Guild("other", "Ajeno", UUID.randomUUID());
        otherGuild.vault().restoreBalance(9000);
        Player otherPlayer = mock(Player.class);
        when(otherPlayer.getUniqueId()).thenReturn(otherGuild.founderId());
        when(guilds.findByMember(playerId)).thenReturn(Optional.of(ownGuild));
        when(guilds.findByMember(otherGuild.founderId())).thenReturn(Optional.of(otherGuild));

        assertEquals("10.50", value("guild_balance"));
        assertEquals("9000", placeholders.onPlaceholderRequest(otherPlayer, "guild_balance"));
        assertEquals("", value("guild_balance_other"));
    }

    @Test
    void soloPlayerAndTemporaryTeamHaveNoGuildTreasury() {
        assertEquals("no", value("in_guild"));
        assertEquals("no", value("in_team"));
        assertEquals("0", value("guild_balance"));

        Team team = new Team(playerId);
        when(teams.getTeam(playerId)).thenReturn(Optional.of(team));
        assertEquals("si", value("in_team"));
        assertEquals("Equipo", value("team_name"));
        assertEquals("1", value("team_size"));
        assertEquals("no", value("in_guild"));
        assertEquals("0", value("guild_balance"));
    }

    @Test
    void guildAndTeamCanBePresentTogether() {
        when(guilds.findByMember(playerId)).thenReturn(Optional.of(new Guild("guild", "Gremio", playerId)));
        when(teams.getTeam(playerId)).thenReturn(Optional.of(new Team(playerId)));

        assertEquals("si", value("in_guild"));
        assertEquals("si", value("in_team"));
        assertEquals("0", value("guild_balance"));
        assertEquals("", placeholders.onPlaceholderRequest(null, "guild_balance"));
    }

    @Test
    void rankUsesTheTranslatedRoleNameAndRoleKeepsTheConstant() {
        GuildsPlaceholders translated = new GuildsPlaceholders(mock(Plugin.class), teams, guilds,
                role -> role.name().equals("LEADER") ? "Líder" : "?");
        when(guilds.findByMember(playerId)).thenReturn(Optional.of(new Guild("guild", "Gremio", playerId)));
        when(teams.getTeam(playerId)).thenReturn(Optional.of(new Team(playerId)));

        assertEquals("Líder", translated.onPlaceholderRequest(player, "guild_rank"));
        assertEquals("Líder", translated.onPlaceholderRequest(player, "team_rank"));
        assertEquals("LEADER", translated.onPlaceholderRequest(player, "guild_role"));
        when(guilds.findByMember(playerId)).thenReturn(Optional.empty());
        assertEquals("-", translated.onPlaceholderRequest(player, "guild_rank"));
    }

    private String value(String key) {
        return placeholders.onPlaceholderRequest(player, key);
    }
}
