package com.sack.rpgroll.crafting.api.event;

import com.sack.rpgroll.crafting.station.CustomStation;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cuando un jugador hace click derecho en el bloque de una estación — cancelable. */
public class StationOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final CustomStation station;
    private final String stationKey;
    private boolean cancelled;

    public StationOpenEvent(Player player, CustomStation station, String stationKey) {
        this.player = player;
        this.station = station;
        this.stationKey = stationKey;
    }

    public Player player() {
        return player;
    }

    public CustomStation station() {
        return station;
    }

    public String stationKey() {
        return stationKey;
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
