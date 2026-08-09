package com.sack.rpgroll.crafting.api.event;

import com.sack.rpgroll.crafting.recipe.CustomRecipe;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cuando una receta empieza a procesarse (ingredientes ya consumidos, costo ya cobrado). */
public class CraftStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final CustomRecipe recipe;
    private final String stationKey;

    public CraftStartEvent(Player player, CustomRecipe recipe, String stationKey) {
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
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
