package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.vanilla.VanillaRecipeDefinition;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/**
 * Solo lectura + borrado: {@code shape}/{@code key}/{@code ingredients} son
 * demasiado ricos para editar campo a campo en un inventario — se crean y
 * editan directamente en {@code vanilla-recipes/*.yml} (mismo criterio que
 * usa RPGRoll-Items para sus propias recetas vanilla).
 */
public class VanillaRecipeBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int BACK_SLOT = 44;

    private final VanillaRecipeManager recipeManager;
    private final Runnable onBack;
    private List<VanillaRecipeDefinition> recipes;

    public VanillaRecipeBrowserGUI(Player player, VanillaRecipeManager recipeManager, Runnable onBack) {
        super(player, Component.text("Recetas vanilla registradas", NamedTextColor.GOLD), SIZE);
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

            VanillaRecipeDefinition recipe = recipes.get(i);

            setItem(i, new ItemBuilder(Material.CRAFTING_TABLE)
                    .setName(Component.text(recipe.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text("tipo: " + recipe.type(), NamedTextColor.AQUA),
                            Component.text("resultado: " + recipe.result().value(), NamedTextColor.GRAY),
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
