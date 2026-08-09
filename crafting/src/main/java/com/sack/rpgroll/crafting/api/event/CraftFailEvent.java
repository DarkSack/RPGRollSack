package com.sack.rpgroll.crafting.api.event;

import com.sack.rpgroll.crafting.recipe.CustomRecipe;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cuando una receta termina su procesamiento pero falla (ver {@code fail-chance}). */
public class CraftFailEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CustomRecipe recipe;
    private final String stationKey;

    public CraftFailEvent(CustomRecipe recipe, String stationKey) {
        this.recipe = recipe;
        this.stationKey = stationKey;
    }

    public CustomRecipe recipe() {
        return recipe;
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
