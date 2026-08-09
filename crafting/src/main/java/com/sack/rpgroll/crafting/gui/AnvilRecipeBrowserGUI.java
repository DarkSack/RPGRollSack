package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.anvil.AnvilRecipeDefinition;
import com.sack.rpgroll.crafting.anvil.AnvilRecipeManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/** Solo lectura + borrado — se crean/editan en {@code anvil-recipes/*.yml} (base+addition+result). */
public class AnvilRecipeBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int BACK_SLOT = 44;

    private final AnvilRecipeManager recipeManager;
    private final Runnable onBack;
    private List<AnvilRecipeDefinition> recipes;

    public AnvilRecipeBrowserGUI(Player player, AnvilRecipeManager recipeManager, Runnable onBack) {
        super(player, Component.text("Recetas de yunque", NamedTextColor.GOLD), SIZE);
        this.recipeManager = recipeManager;
        this.onBack = onBack;
        this.recipes = List.copyOf(recipeManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < recipes.size() && i < 36; i++) {

            AnvilRecipeDefinition recipe = recipes.get(i);

            setItem(i, new ItemBuilder(Material.ANVIL)
                    .setName(Component.text(recipe.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text("base: " + recipe.baseIngredient().value(), NamedTextColor.GRAY),
                            Component.text("adición: " + recipe.additionIngredient().value(), NamedTextColor.GRAY),
                            Component.text("resultado: " + recipe.result().value(), NamedTextColor.AQUA),
                            Component.text("Click para eliminar", NamedTextColor.RED))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < recipes.size() && slot < 36) {
            recipeManager.delete(recipes.get(slot).id());
            reopen();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void reopen() {
        this.recipes = List.copyOf(recipeManager.getAll());
        open();
    }

}
