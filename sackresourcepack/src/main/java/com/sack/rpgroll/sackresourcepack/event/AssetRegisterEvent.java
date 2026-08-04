package com.sack.rpgroll.sackresourcepack.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cuando un plugin registra (o actualiza) un módulo de assets, vía jar-scan o la API programática. */
public class AssetRegisterEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String pluginName;
    private final String moduleId;

    public AssetRegisterEvent(String pluginName, String moduleId) {
        this.pluginName = pluginName;
        this.moduleId = moduleId;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getModuleId() {
        return moduleId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
