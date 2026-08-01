package com.sack.rpgroll.gameplay.listener;

import com.sack.rpgroll.gameplay.event.LevelUpEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listener para eventos de level up.
 */
public class LevelUpListener implements Listener {

    @EventHandler
    public void onLevelUp(LevelUpEvent event) {

        Player player = event.getPlayer();
        int newLevel = event.getNewLevel();
        var rewards = event.getRewards();

        // Anunciar a todos los jugadores
        Bukkit.broadcast(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        Bukkit.broadcast(
                Component.text("✦ ", NamedTextColor.YELLOW)
                        .append(Component.text(player.getName(), NamedTextColor.GOLD))
                        .append(Component.text(" ha alcanzado el nivel ", NamedTextColor.YELLOW))
                        .append(Component.text(String.valueOf(newLevel), NamedTextColor.GOLD))
                        .append(Component.text(" ✦", NamedTextColor.YELLOW)));

        if (!rewards.getSummary().isEmpty()) {
            Bukkit.broadcast(Component.text("Recompensas: ", NamedTextColor.AQUA)
                    .append(Component.text(rewards.getSummary(), NamedTextColor.WHITE)));
        }

        Bukkit.broadcast(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    }

}
