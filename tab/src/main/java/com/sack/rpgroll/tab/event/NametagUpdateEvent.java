package com.sack.rpgroll.tab.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cada vez que se re-renderiza el nametag de un jugador. */
public class NametagUpdateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String nametagId;

    public NametagUpdateEvent(Player player, String nametagId) {
        this.player = player;
        this.nametagId = nametagId;
    }

    public Player player() {
        return player;
    }

    public String nametagId() {
        return nametagId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
