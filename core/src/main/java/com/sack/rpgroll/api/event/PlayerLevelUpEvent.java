package com.sack.rpgroll.api.event;

import com.sack.rpgroll.player.RPGPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Se dispara cuando un jugador sube de nivel de personaje (no nivel de
 * trabajo — ver PlayerJobLevelUpEvent para eso).
 */
public class PlayerLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final RPGPlayer rpgPlayer;
    private final int previousLevel;
    private final int newLevel;

    public PlayerLevelUpEvent(Player player, RPGPlayer rpgPlayer, int previousLevel, int newLevel) {
        this.player = player;
        this.rpgPlayer = rpgPlayer;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public RPGPlayer getRpgPlayer() {
        return rpgPlayer;
    }

    public int getPreviousLevel() {
        return previousLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}