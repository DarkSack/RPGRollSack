package com.sack.rpgroll.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Un jugador va a elegir su raza. Ver {@link CharacterSelectionEvent}. */
public class PlayerSelectRaceEvent extends CharacterSelectionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public PlayerSelectRaceEvent(Player player, String raceId, Source source) {
        super(player, raceId, source);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
