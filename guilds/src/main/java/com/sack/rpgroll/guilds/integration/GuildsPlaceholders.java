package com.sack.rpgroll.guilds.integration;

import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/** %rpgrollguilds_&lt;placeholder&gt;% — equipo y guild del jugador que consulta el placeholder. */
public class GuildsPlaceholders extends PlaceholderExpansion {

    private final Plugin plugin;
    private final TeamManager teamManager;
    private final GuildManager guildManager;

    public GuildsPlaceholders(Plugin plugin, TeamManager teamManager, GuildManager guildManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.guildManager = guildManager;
    }

    @Override
    public String getIdentifier() {
        return "rpgrollguilds";
    }

    @Override
    public String getAuthor() {
        return "Sack";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {

        String key = params.toLowerCase(Locale.ROOT);

        if (key.equals("guilds_count")) {
            return String.valueOf(guildManager.count());
        }

        if (player == null) {
            return "";
        }

        Team team = teamManager.getTeam(player.getUniqueId()).orElse(null);
        Guild guild = guildManager.findByMember(player.getUniqueId()).orElse(null);

        return switch (key) {
            case "in_team" -> team != null ? "si" : "no";
            case "team_name" -> team != null ? team.name() : "-";
            case "team_size" -> team != null ? String.valueOf(team.size()) : "0";
            case "team_role" -> team != null ? team.roleOf(player.getUniqueId()).toString() : "-";
            case "in_guild" -> guild != null ? "si" : "no";
            case "guild_name" -> guild != null ? guild.name() : "-";
            case "guild_id" -> guild != null ? guild.id() : "-";
            case "guild_level" -> guild != null ? String.valueOf(guild.level()) : "0";
            case "guild_role" -> guild != null ? guild.roleOf(player.getUniqueId()).toString() : "-";
            case "guild_members" -> guild != null ? String.valueOf(guild.memberCount()) : "0";
            default -> "";
        };
    }

}
