package com.sack.rpgroll.gameplay.event;

import com.sack.rpgroll.gameplay.levelup.LevelUpRewards;
import com.sack.rpgroll.player.RPGPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento disparado cuando un jugador sube de nivel.
 */
public class LevelUpEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final RPGPlayer rpgPlayer;
    private final int newLevel;
    private final LevelUpRewards rewards;

    public LevelUpEvent(Player player, RPGPlayer rpgPlayer, int newLevel, LevelUpRewards rewards) {
        this.player = player;
        this.rpgPlayer = rpgPlayer;
        this.newLevel = newLevel;
        this.rewards = rewards;
    }

    public Player getPlayer() {
        return player;
    }

    public RPGPlayer getRPGPlayer() {
        return rpgPlayer;
    }

    public int getNewLevel() {
        return newLevel;
    }

    public LevelUpRewards getRewards() {
        return rewards;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
