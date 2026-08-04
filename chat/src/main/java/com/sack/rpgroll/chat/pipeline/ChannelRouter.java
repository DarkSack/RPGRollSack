package com.sack.rpgroll.chat.pipeline;

import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.channel.ChannelScope;
import com.sack.rpgroll.chat.ignore.IgnoreManager;
import com.sack.rpgroll.chat.proximity.ProximityCalculator;
import com.sack.rpgroll.guilds.GuildsAPI;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Resuelve los destinatarios de un canal — el paso "Guild/Team/Región/Canal" del pipeline. */
public class ChannelRouter {

    private final IgnoreManager ignoreManager;

    public ChannelRouter(IgnoreManager ignoreManager) {
        this.ignoreManager = ignoreManager;
    }

    public List<Player> resolve(Player sender, ChatChannel channel) {

        List<Player> recipients = new ArrayList<>();

        for (Player candidate : candidatesFor(sender, channel)) {

            if (channel.requiresViewPermission() && !candidate.hasPermission(channel.viewPermission())) {
                continue;
            }

            if (!candidate.equals(sender) && ignoreManager.blocks(candidate, sender, channel.id())) {
                continue;
            }

            recipients.add(candidate);
        }

        return recipients;
    }

    private List<Player> candidatesFor(Player sender, ChatChannel channel) {

        return switch (channel.scope()) {

            case GLOBAL -> channel.crossWorld()
                    ? new ArrayList<>(Bukkit.getOnlinePlayers())
                    : Bukkit.getOnlinePlayers().stream().map(p -> (Player) p)
                            .filter(p -> p.getWorld().equals(sender.getWorld())).toList();

            case WORLD -> Bukkit.getOnlinePlayers().stream().map(p -> (Player) p)
                    .filter(p -> p.getWorld().equals(sender.getWorld()))
                    .toList();

            case PROXIMITY -> Bukkit.getOnlinePlayers().stream().map(p -> (Player) p)
                    .filter(p -> ProximityCalculator.canHear(sender, p, channel))
                    .toList();

            case STAFF -> new ArrayList<>(Bukkit.getOnlinePlayers());

            case GUILD -> guildMembers(sender);

            case TEAM -> teamMembers(sender);
        };
    }

    private List<Player> guildMembers(Player sender) {

        if (!GuildsAPI.isReady()) {
            return List.of(sender);
        }

        return GuildsAPI.getGuildManager().findByMember(sender.getUniqueId())
                .map(guild -> guild.members().keySet().stream()
                        .map(Bukkit::getPlayer)
                        .filter(java.util.Objects::nonNull)
                        .toList())
                .orElse(List.of(sender));
    }

    private List<Player> teamMembers(Player sender) {

        if (!GuildsAPI.isReady()) {
            return List.of(sender);
        }

        return GuildsAPI.getTeamManager().getTeam(sender.getUniqueId())
                .map(team -> team.members().stream()
                        .map(Bukkit::getPlayer)
                        .filter(java.util.Objects::nonNull)
                        .toList())
                .orElse(List.of(sender));
    }

}
