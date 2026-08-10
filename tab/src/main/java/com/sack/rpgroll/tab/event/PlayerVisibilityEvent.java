package com.sack.rpgroll.tab.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara antes de mostrar/ocultar a {@code subject} en el tablist de {@code viewer} — cancelable para vetar el cambio. */
public class PlayerVisibilityEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player viewer;
    private final Player subject;
    private final boolean listed;
    private boolean cancelled;

    public PlayerVisibilityEvent(Player viewer, Player subject, boolean listed) {
        this.viewer = viewer;
        this.subject = subject;
        this.listed = listed;
    }

    public Player viewer() {
        return viewer;
    }

    public Player subject() {
        return subject;
    }

    public boolean listed() {
        return listed;
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
