package com.sack.rpgroll.crafting.api.event;

import com.sack.rpgroll.crafting.recipe.CustomRecipe;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara la primera vez que un jugador descubre una {@code CustomRecipe} (ver Recipe Book). */
public class RecipeDiscoverEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final CustomRecipe recipe;

    public RecipeDiscoverEvent(Player player, CustomRecipe recipe) {
        this.player = player;
        this.recipe = recipe;
    }

    public Player player() {
        return player;
    }

    public CustomRecipe recipe() {
        return recipe;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
