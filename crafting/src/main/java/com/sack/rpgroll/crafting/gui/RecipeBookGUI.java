package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.discovery.DiscoveryService;
import com.sack.rpgroll.crafting.recipe.CustomRecipe;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

/**
 * Libro de recetas del jugador: muestra todas las {@code CustomRecipe} que
 * ya descubrió con su ícono e ingredientes reales, y las que no con un
 * ítem "???" — recorrer las recetas nunca revela una todavía no descubierta.
 */
public class RecipeBookGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private final CustomRecipeManager recipeManager;
    private final CustomStationManager stationManager;
    private final DiscoveryService discoveryService;
    private final List<CustomRecipe> recipes;

    public RecipeBookGUI(Player player, CustomRecipeManager recipeManager, CustomStationManager stationManager,
            DiscoveryService discoveryService) {
        super(player, Component.text("Libro de Recetas", NamedTextColor.DARK_AQUA), SIZE);
        this.recipeManager = recipeManager;
        this.stationManager = stationManager;
        this.discoveryService = discoveryService;
        this.recipes = List.copyOf(recipeManager.getAll());
    }

    @Override
    public void build() {

        clear();

        var discovery = discoveryService.get(player.getUniqueId());

        for (int i = 0; i < recipes.size() && i < SIZE; i++) {

            CustomRecipe recipe = recipes.get(i);

            if (!discovery.hasDiscovered(recipe.id())) {
                setItem(i, new ItemBuilder(Material.BARRIER)
                        .setName(Component.text("???", NamedTextColor.DARK_GRAY))
                        .setLore(Component.text("Receta sin descubrir todavía.", NamedTextColor.GRAY))
                        .build());
                continue;
            }

            String stationName = stationManager.get(recipe.stationId())
                    .map(com.sack.rpgroll.crafting.station.CustomStation::displayName)
                    .orElse(recipe.stationId());

            setItem(i, new ItemBuilder(parseMaterial(recipe.icon()))
                    .setName(Component.text(recipe.displayName(), NamedTextColor.AQUA))
                    .setLore(Component.text("Estación: " + stationName, NamedTextColor.GRAY),
                            Component.text("Resultado: " + recipe.result().value() + " x" + recipe.result().amount(),
                                    NamedTextColor.YELLOW),
                            Component.text("Ingredientes: " + recipe.ingredients().size(), NamedTextColor.GRAY))
                    .build());
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    private Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.BOOK;
        }
    }

}
