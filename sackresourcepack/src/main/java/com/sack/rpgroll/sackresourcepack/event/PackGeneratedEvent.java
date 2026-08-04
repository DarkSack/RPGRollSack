package com.sack.rpgroll.sackresourcepack.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.io.File;

/** Se dispara después de generar el ZIP con éxito (fuera o no de la caché incremental). */
public class PackGeneratedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final File zipFile;
    private final String sha1;
    private final boolean fromCache;

    public PackGeneratedEvent(File zipFile, String sha1, boolean fromCache) {
        this.zipFile = zipFile;
        this.sha1 = sha1;
        this.fromCache = fromCache;
    }

    public File getZipFile() {
        return zipFile;
    }

    public String getSha1() {
        return sha1;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
