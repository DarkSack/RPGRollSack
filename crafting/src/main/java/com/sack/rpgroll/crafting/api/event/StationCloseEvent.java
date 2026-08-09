package com.sack.rpgroll.crafting.api.event;

import com.sack.rpgroll.crafting.station.CustomStation;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cuando un jugador cierra el inventario de una estación. */
public class StationCloseEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final CustomStation station;
    private final String stationKey;

    public StationCloseEvent(Player player, CustomStation station, String stationKey) {
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
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
