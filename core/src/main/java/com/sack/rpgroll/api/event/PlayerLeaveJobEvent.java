package com.sack.rpgroll.api.event;

import com.sack.rpgroll.player.RPGPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerLeaveJobEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final RPGPlayer rpgPlayer;
    private final String jobId;

    public PlayerLeaveJobEvent(Player player, RPGPlayer rpgPlayer, String jobId) {
        this.player = player;
        this.rpgPlayer = rpgPlayer;
        this.jobId = jobId;
    }

    public Player getPlayer() {
        return player;
    }

    public RPGPlayer getRpgPlayer() {
        return rpgPlayer;
    }

    public String getJobId() {
        return jobId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}