package com.sack.rpgroll.api.event;

import com.sack.rpgroll.player.RPGPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Se dispara cuando un jugador intenta unirse a un trabajo. Cancelable —
 * un addon puede bloquear el join (ej. requisito de nivel mínimo que
 * RPGRoll core no conoce).
 */
public class PlayerJoinJobEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final RPGPlayer rpgPlayer;
    private final String jobId;
    private boolean cancelled;

    public PlayerJoinJobEvent(Player player, RPGPlayer rpgPlayer, String jobId) {
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
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}