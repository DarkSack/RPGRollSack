package com.sack.rpgroll.chat.mention;

import com.sack.rpgroll.guilds.GuildsAPI;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detecta @Jugador / @Guild / @Team / @All — spec "Mention System". */
public class MentionResolver {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    public record MentionResult(Set<Player> mentionedPlayers, boolean mentionsAll) {
    }

    public MentionResult resolve(String message, Player sender) {

        Set<Player> mentioned = new LinkedHashSet<>();
        boolean mentionsAll = false;

        Matcher matcher = MENTION_PATTERN.matcher(message);

        while (matcher.find()) {

            String token = matcher.group(1);

            if (token.equalsIgnoreCase("all") || token.equalsIgnoreCase("todos")) {
                mentionsAll = true;
                continue;
            }

            if (token.equalsIgnoreCase("guild") && GuildsAPI.isReady()) {
                GuildsAPI.getGuildManager().findByMember(sender.getUniqueId()).ifPresent(guild ->
                        guild.members().keySet().forEach(uuid -> {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null) {
                                mentioned.add(player);
                            }
                        }));
                continue;
            }

            if (token.equalsIgnoreCase("team") && GuildsAPI.isReady()) {
                GuildsAPI.getTeamManager().getTeam(sender.getUniqueId()).ifPresent(team ->
                        team.members().forEach(uuid -> {
                            Player player = Bukkit.getPlayer(uuid);
                            if (player != null) {
                                mentioned.add(player);
                            }
                        }));
                continue;
            }

            Player target = Bukkit.getPlayerExact(token);
            if (target != null) {
                mentioned.add(target);
            }
        }

        return new MentionResult(mentioned, mentionsAll);
    }

    /** Resalta las menciones en el texto (solo para canales en formato LEGACY). */
    public String highlight(String message) {
        return MENTION_PATTERN.matcher(message).replaceAll("&e&l@$1&r&f");
    }

}
