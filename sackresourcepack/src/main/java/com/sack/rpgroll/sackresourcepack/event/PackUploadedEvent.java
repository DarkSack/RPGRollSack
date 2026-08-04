package com.sack.rpgroll.sackresourcepack.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara después de intentar subir el pack a un endpoint remoto (ver remote-upload en config.yml). */
public class PackUploadedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final boolean success;
    private final String message;

    public PackUploadedEvent(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
