package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.anvil.AnvilRecipeManager;
import com.sack.rpgroll.crafting.brewing.BrewRecipeManager;
import com.sack.rpgroll.crafting.fuel.FuelManager;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeBridge;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Menú principal de administración de RPGRoll-Crafting (Crafting Studio). */
public class CraftingStudioHubGUI extends InventoryGUI {

    private static final int SIZE = 27;

    private static final int STATIONS_SLOT = 10;
    private static final int RECIPES_SLOT = 11;
    private static final int FUELS_SLOT = 12;
    private static final int VANILLA_RECIPES_SLOT = 14;
    private static final int ANVIL_RECIPES_SLOT = 15;
    private static final int BREW_RECIPES_SLOT = 16;
    private static final int CLOSE_SLOT = 22;

    private final CustomStationManager stationManager;
    private final CustomRecipeManager recipeManager;
    private final FuelManager fuelManager;
    private final VanillaRecipeManager vanillaRecipeManager;
    private final AnvilRecipeManager anvilRecipeManager;
    private final BrewRecipeManager brewRecipeManager;
    private final VanillaRecipeBridge vanillaRecipeBridge;
    private final ChatPromptManager chatPromptManager;

    public CraftingStudioHubGUI(Player player, CustomStationManager stationManager, CustomRecipeManager recipeManager,
            FuelManager fuelManager, VanillaRecipeManager vanillaRecipeManager, AnvilRecipeManager anvilRecipeManager,
            BrewRecipeManager brewRecipeManager, VanillaRecipeBridge vanillaRecipeBridge,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text("Crafting Studio", NamedTextColor.DARK_AQUA), SIZE);
        this.stationManager = stationManager;
        this.recipeManager = recipeManager;
        this.fuelManager = fuelManager;
        this.vanillaRecipeManager = vanillaRecipeManager;
        this.anvilRecipeManager = anvilRecipeManager;
        this.brewRecipeManager = brewRecipeManager;
        this.vanillaRecipeBridge = vanillaRecipeBridge;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(STATIONS_SLOT, new ItemBuilder(Material.SMITHING_TABLE)
                .setName(Component.text("Estaciones personalizadas", NamedTextColor.YELLOW))
                .setLore(Component.text(stationManager.count() + " definidas", NamedTextColor.GRAY)).build());

        setItem(RECIPES_SLOT, new ItemBuilder(Material.CRAFTING_TABLE)
                .setName(Component.text("Recetas personalizadas", NamedTextColor.YELLOW))
                .setLore(Component.text(recipeManager.count() + " definidas", NamedTextColor.GRAY)).build());

        setItem(FUELS_SLOT, new ItemBuilder(Material.COAL)
                .setName(Component.text("Combustibles", NamedTextColor.YELLOW))
                .setLore(Component.text(fuelManager.count() + " definidos", NamedTextColor.GRAY)).build());

        setItem(VANILLA_RECIPES_SLOT, new ItemBuilder(Material.FURNACE)
                .setName(Component.text("Recetas vanilla", NamedTextColor.YELLOW))
                .setLore(Component.text(vanillaRecipeManager.count() + " registradas", NamedTextColor.GRAY)).build());

        setItem(ANVIL_RECIPES_SLOT, new ItemBuilder(Material.ANVIL)
                .setName(Component.text("Recetas de yunque", NamedTextColor.YELLOW))
                .setLore(Component.text(anvilRecipeManager.count() + " definidas", NamedTextColor.GRAY)).build());

        setItem(BREW_RECIPES_SLOT, new ItemBuilder(Material.BREWING_STAND)
                .setName(Component.text("Recetas de fermentación", NamedTextColor.YELLOW))
                .setLore(Component.text(brewRecipeManager.count() + " definidas", NamedTextColor.GRAY)).build());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == STATIONS_SLOT) {
            new CustomStationBrowserGUI(player, stationManager, chatPromptManager, this::reopen).open();
        } else if (slot == RECIPES_SLOT) {
            new CustomRecipeBrowserGUI(player, recipeManager, chatPromptManager, this::reopen).open();
        } else if (slot == FUELS_SLOT) {
            new FuelBrowserGUI(player, fuelManager, chatPromptManager, this::reopen).open();
        } else if (slot == VANILLA_RECIPES_SLOT) {
            new VanillaRecipeBrowserGUI(player, vanillaRecipeManager, vanillaRecipeBridge, chatPromptManager,
                    this::reopen).open();
        } else if (slot == ANVIL_RECIPES_SLOT) {
            new AnvilRecipeBrowserGUI(player, anvilRecipeManager, chatPromptManager, this::reopen).open();
        } else if (slot == BREW_RECIPES_SLOT) {
            new BrewRecipeBrowserGUI(player, brewRecipeManager, chatPromptManager, this::reopen).open();
        } else if (slot == CLOSE_SLOT) {
            close();
        }
    }

    private void reopen() {
        open();
    }

}
