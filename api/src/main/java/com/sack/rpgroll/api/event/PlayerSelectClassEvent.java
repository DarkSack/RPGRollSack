package com.sack.rpgroll.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/** Un jugador va a elegir su clase. Ver {@link CharacterSelectionEvent}. */
public class PlayerSelectClassEvent extends CharacterSelectionEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public PlayerSelectClassEvent(Player player, String classId, Source source) {
        super(player, classId, source);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
