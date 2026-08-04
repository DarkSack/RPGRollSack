package com.sack.rpgroll.sackresourcepack.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cada vez que se le manda el pack a un jugador (al entrar, o tras un /srp rebuild que reenvía a todos). */
public class PackSentEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    public PackSentEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
