package com.sack.rpgroll.gameplay.listener;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.event.LevelUpEvent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listener para eventos de level up.
 */
public class LevelUpListener implements Listener {

    private final LangManager lang;

    public LevelUpListener(LangManager lang) {
        this.lang = lang;
    }

    @EventHandler
    public void onLevelUp(LevelUpEvent event) {

        Player player = event.getPlayer();
        int newLevel = event.getNewLevel();
        var rewards = event.getRewards();

        // Anunciar a todos los jugadores
        Bukkit.broadcast(lang.component("level_up_listener.border"));
        Bukkit.broadcast(lang.component("level_up_listener.announcement", "player", player.getName(), "level",
                newLevel));

        if (!rewards.getSummary().isEmpty()) {
            Bukkit.broadcast(lang.component("level_up_listener.rewards", "summary", rewards.getSummary()));
        }

        Bukkit.broadcast(lang.component("level_up_listener.border"));
    }

}
