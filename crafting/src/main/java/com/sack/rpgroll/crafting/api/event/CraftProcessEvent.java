package com.sack.rpgroll.crafting.api.event;

import com.sack.rpgroll.crafting.recipe.CustomRecipe;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara en cada tick que una estación avanza el progreso de una receta en curso. */
public class CraftProcessEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CustomRecipe recipe;
    private final String stationKey;
    private final int progressTicks;
    private final int totalTicks;

    public CraftProcessEvent(CustomRecipe recipe, String stationKey, int progressTicks, int totalTicks) {
        this.recipe = recipe;
        this.stationKey = stationKey;
        this.progressTicks = progressTicks;
        this.totalTicks = totalTicks;
    }

    public CustomRecipe recipe() {
        return recipe;
    }

    public String stationKey() {
        return stationKey;
    }

    public int progressTicks() {
        return progressTicks;
    }

    public int totalTicks() {
        return totalTicks;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
