package com.sack.rpgroll.crafting.api.event;

import com.sack.rpgroll.crafting.recipe.CustomRecipe;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Se dispara justo antes de que {@code StationProcessingEngine} consuma
 * ingredientes y cobre el costo de una receta que ya pasó sus condiciones e
 * ingredientes disponibles — cancelable para que otro plugin la bloquee en
 * el último momento (ej. un cooldown externo).
 */
public class CraftPrepareEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final CustomRecipe recipe;
    private final String stationKey;
    private boolean cancelled;

    public CraftPrepareEvent(Player player, CustomRecipe recipe, String stationKey) {
        this.player = player;
        this.recipe = recipe;
        this.stationKey = stationKey;
    }

    public Player player() {
        return player;
    }

    public CustomRecipe recipe() {
        return recipe;
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
