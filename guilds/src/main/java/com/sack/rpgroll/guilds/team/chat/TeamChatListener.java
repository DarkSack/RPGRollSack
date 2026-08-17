package com.sack.rpgroll.guilds.team.chat;

import com.sack.rpgroll.guilds.GuildsAPI;
import com.sack.rpgroll.guilds.team.Team;
import com.sack.rpgroll.guilds.team.TeamManager;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Canal de chat privado de equipo — spec: "canal de chat de equipo privado", alternado con /team chat. */
public class TeamChatListener implements Listener {

    private final TeamManager teamManager;
    private final Set<UUID> chatModeEnabled = ConcurrentHashMap.newKeySet();

    public TeamChatListener(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public boolean toggle(Player player) {
        if (!chatModeEnabled.remove(player.getUniqueId())) {
            chatModeEnabled.add(player.getUniqueId());
            return true;
        }
        return false;
    }

    public void disable(UUID playerId) {
        chatModeEnabled.remove(playerId);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {

        Player player = event.getPlayer();

        if (!chatModeEnabled.contains(player.getUniqueId())) {
            return;
        }

        Team team = teamManager.getTeam(player.getUniqueId()).orElse(null);

        if (team == null) {
            chatModeEnabled.remove(player.getUniqueId());
            return;
        }

        event.setCancelled(true);

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        Component formatted = Component.text("[" + GuildsAPI.getLangManager().raw("team.chat.tag") + "] ",
                        NamedTextColor.AQUA)
                .append(Component.text(player.getName() + ": ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));

        for (UUID memberId : team.members()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(formatted);
            }
        }
    }

}
