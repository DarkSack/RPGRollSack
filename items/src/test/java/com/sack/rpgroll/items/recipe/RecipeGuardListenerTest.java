package com.sack.rpgroll.items.recipe;

import com.sack.rpgroll.items.instance.ItemInstanceService;

import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeGuardListenerTest {

    @Test
    void emptySlotsInTheCraftingGridDoNotBreakTheGuard() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("RPGRoll-Items");
        when(plugin.namespace()).thenReturn("rpgroll-items");
        RecipeGuardListener guard = new RecipeGuardListener(plugin, mock(ItemInstanceService.class), () -> true);

        ShapedRecipe recipe = mock(ShapedRecipe.class);
        when(recipe.getKey()).thenReturn(NamespacedKey.minecraft("stick"));
        CraftingInventory inventory = mock(CraftingInventory.class);
        // La mesa de crafteo devuelve null en cada casilla vacía.
        when(inventory.getMatrix()).thenReturn(new ItemStack[9]);
        PrepareItemCraftEvent event = mock(PrepareItemCraftEvent.class);
        when(event.getRecipe()).thenReturn(recipe);
        when(event.getInventory()).thenReturn(inventory);

        assertDoesNotThrow(() -> guard.onPrepareCraft(event));
        verify(inventory, never()).setResult(any());
    }
}
