package com.sack.rpgroll.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerJobLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String jobId;
    private final int previousLevel;
    private final int newLevel;

    public PlayerJobLevelUpEvent(Player player, String jobId, int previousLevel, int newLevel) {
        this.player = player;
        this.jobId = jobId;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public String getJobId() {
        return jobId;
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