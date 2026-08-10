package com.sack.rpgroll.tab.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Informativo: se dispara cada vez que se resuelve un placeholder desconocido
 * (ni interno ni registrado) — útil para depurar YAMLs con placeholders mal
 * escritos. Para REGISTRAR placeholders usá
 * {@link com.sack.rpgroll.tab.placeholder.TABPlaceholderRegistry}, no este evento.
 */
public class PlaceholderRequestEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String key;

    public PlaceholderRequestEvent(Player player, String key) {
        this.player = player;
        this.key = key;
    }

    public Player player() {
        return player;
    }

    public String key() {
        return key;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
