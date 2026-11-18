package com.sack.rpgroll.guilds.integration;

import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.function.Function;

/**
 * %rpgrollguilds_&lt;placeholder&gt;% — equipo y guild del jugador que consulta el placeholder.
 * {@code guild_balance} lee su tesorería compartida, en la moneda usada por Vault.
 */
public class GuildsPlaceholders extends PlaceholderExpansion {

    private final Plugin plugin;
    private final TeamManager teamManager;
    private final GuildManager guildManager;
    private final Function<Enum<?>, String> roleNames;

    public GuildsPlaceholders(Plugin plugin, TeamManager teamManager, GuildManager guildManager) {
        this(plugin, teamManager, guildManager, Enum::name);
    }

    /**
     * @param roleNames nombre de un rol en el idioma del servidor, para {@code guild_rank} y
     *                  {@code team_rank} (los {@code *_role} siguen dando la constante, para condiciones).
     */
    public GuildsPlaceholders(Plugin plugin, TeamManager teamManager, GuildManager guildManager,
            Function<Enum<?>, String> roleNames) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.guildManager = guildManager;
        this.roleNames = roleNames;
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
            case "team_rank" -> team != null ? roleNames.apply(team.roleOf(player.getUniqueId())) : "-";
            case "in_guild" -> guild != null ? "si" : "no";
            case "guild_name" -> guild != null ? guild.name() : "-";
            case "guild_id" -> guild != null ? guild.id() : "-";
            case "guild_level" -> guild != null ? String.valueOf(guild.level()) : "0";
            case "guild_role" -> guild != null ? guild.roleOf(player.getUniqueId()).toString() : "-";
            case "guild_rank" -> guild != null ? roleNames.apply(guild.roleOf(player.getUniqueId())) : "-";
            case "guild_members" -> guild != null ? String.valueOf(guild.memberCount()) : "0";
            case "guild_balance" -> guild != null ? formatNumber(guild.vault().balance()) : "0";
            default -> "";
        };
    }

    private String formatNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.2f", value);
    }

}
