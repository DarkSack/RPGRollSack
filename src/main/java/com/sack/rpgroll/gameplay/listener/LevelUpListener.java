package com.sack.rpgroll.gameplay.listener;

import com.sack.rpgroll.gameplay.event.LevelUpEvent;
import org.bukkit.ChatColor;
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
        Bukkit.broadcastMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "✦ " + ChatColor.GOLD + player.getName() +
                ChatColor.YELLOW + " ha alcanzado el nivel " + ChatColor.GOLD + newLevel +
                ChatColor.YELLOW + " ✦");

        if (!rewards.getSummary().isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.AQUA + "Recompensas: " + ChatColor.WHITE + rewards.getSummary());
        }

        Bukkit.broadcastMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

}
