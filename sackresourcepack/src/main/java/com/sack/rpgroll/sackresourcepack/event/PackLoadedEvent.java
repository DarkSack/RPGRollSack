package com.sack.rpgroll.sackresourcepack.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara una sola vez al terminar el escaneo+build inicial del arranque del plugin. */
public class PackLoadedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int moduleCount;

    public PackLoadedEvent(int moduleCount) {
        this.moduleCount = moduleCount;
    }

    public int getModuleCount() {
        return moduleCount;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
