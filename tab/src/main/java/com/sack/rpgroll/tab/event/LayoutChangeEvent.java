package com.sack.rpgroll.tab.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cuando cambia el layout/orden de tablist aplicado a un jugador. */
public class LayoutChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String layoutId;

    public LayoutChangeEvent(Player player, String layoutId) {
        this.player = player;
        this.layoutId = layoutId;
    }

    public Player player() {
        return player;
    }

    public String layoutId() {
        return layoutId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
