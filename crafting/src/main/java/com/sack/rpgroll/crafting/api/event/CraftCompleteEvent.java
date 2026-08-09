package com.sack.rpgroll.crafting.api.event;

import com.sack.rpgroll.crafting.quality.CraftQuality;
import com.sack.rpgroll.crafting.recipe.CustomRecipe;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/** Se dispara al entregarse exitosamente el resultado de una receta. */
public class CraftCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final CustomRecipe recipe;
    private final ItemStack result;
    private final CraftQuality quality;

    public CraftCompleteEvent(Player player, CustomRecipe recipe, ItemStack result, CraftQuality quality) {
        this.player = player;
        this.recipe = recipe;
        this.result = result;
        this.quality = quality;
    }

    public Player player() {
        return player;
    }

    public CustomRecipe recipe() {
        return recipe;
    }

    public ItemStack result() {
        return result;
    }

    public CraftQuality quality() {
        return quality;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
