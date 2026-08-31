package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.crafting.anvil.AnvilRecipeManager;
import com.sack.rpgroll.crafting.brewing.BrewRecipeManager;
import com.sack.rpgroll.crafting.cartography.CartographyRecipeManager;
import com.sack.rpgroll.crafting.fuel.FuelManager;
import com.sack.rpgroll.crafting.grindstone.GrindstoneRecipeManager;
import com.sack.rpgroll.crafting.loom.LoomRecipeManager;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeBridge;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeManager;
import com.sack.rpgroll.crafting.villager.VillagerTradeManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Menú principal de administración de RPGRoll-Crafting (Crafting Studio). */
public class CraftingStudioHubGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int STATIONS_SLOT = 10;
    private static final int RECIPES_SLOT = 11;
    private static final int FUELS_SLOT = 12;
    private static final int VANILLA_RECIPES_SLOT = 13;
    private static final int ANVIL_RECIPES_SLOT = 14;
    private static final int BREW_RECIPES_SLOT = 15;
    private static final int GRINDSTONE_RECIPES_SLOT = 16;
    private static final int CARTOGRAPHY_RECIPES_SLOT = 17;
    private static final int LOOM_RECIPES_SLOT = 19;
    private static final int VILLAGER_TRADES_SLOT = 20;
    private static final int CLOSE_SLOT = 40;

    private final CustomStationManager stationManager;
    private final CustomRecipeManager recipeManager;
    private final FuelManager fuelManager;
    private final VanillaRecipeManager vanillaRecipeManager;
    private final AnvilRecipeManager anvilRecipeManager;
    private final BrewRecipeManager brewRecipeManager;
    private final GrindstoneRecipeManager grindstoneRecipeManager;
    private final CartographyRecipeManager cartographyRecipeManager;
    private final LoomRecipeManager loomRecipeManager;
    private final VillagerTradeManager villagerTradeManager;
    private final VanillaRecipeBridge vanillaRecipeBridge;
    private final ChatPromptManager chatPromptManager;
    private final com.sack.rpgroll.common.lang.LangManager lang;

    public CraftingStudioHubGUI(Player player, CustomStationManager stationManager, CustomRecipeManager recipeManager,
            FuelManager fuelManager, VanillaRecipeManager vanillaRecipeManager, AnvilRecipeManager anvilRecipeManager,
            BrewRecipeManager brewRecipeManager, GrindstoneRecipeManager grindstoneRecipeManager,
            CartographyRecipeManager cartographyRecipeManager, LoomRecipeManager loomRecipeManager,
            VillagerTradeManager villagerTradeManager, VanillaRecipeBridge vanillaRecipeBridge,
            ChatPromptManager chatPromptManager) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.hub.title"), NamedTextColor.DARK_AQUA), SIZE);
        this.lang = chatPromptManager.lang();
        this.stationManager = stationManager;
        this.recipeManager = recipeManager;
        this.fuelManager = fuelManager;
        this.vanillaRecipeManager = vanillaRecipeManager;
        this.anvilRecipeManager = anvilRecipeManager;
        this.brewRecipeManager = brewRecipeManager;
        this.grindstoneRecipeManager = grindstoneRecipeManager;
        this.cartographyRecipeManager = cartographyRecipeManager;
        this.loomRecipeManager = loomRecipeManager;
        this.villagerTradeManager = villagerTradeManager;
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
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.hub.stations_name"), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.hub.count_defined", "count", stationManager.count()), NamedTextColor.GRAY)).build());

        setItem(RECIPES_SLOT, new ItemBuilder(Material.CRAFTING_TABLE)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.hub.recipes_name"), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.hub.count_defined", "count", recipeManager.count()), NamedTextColor.GRAY)).build());

        setItem(FUELS_SLOT, new ItemBuilder(Material.COAL)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.hub.fuels_name"), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.hub.count_defined_masc", "count", fuelManager.count()), NamedTextColor.GRAY)).build());

        setItem(VANILLA_RECIPES_SLOT, new ItemBuilder(Material.FURNACE)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.hub.vanilla_recipes_name"), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.hub.count_registered", "count", vanillaRecipeManager.count()), NamedTextColor.GRAY)).build());

        setItem(ANVIL_RECIPES_SLOT, new ItemBuilder(Material.ANVIL)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.hub.anvil_recipes_name"), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.hub.count_defined", "count", anvilRecipeManager.count()), NamedTextColor.GRAY)).build());

        setItem(BREW_RECIPES_SLOT, new ItemBuilder(Material.BREWING_STAND)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.hub.brew_recipes_name"), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.hub.count_defined", "count", brewRecipeManager.count()), NamedTextColor.GRAY)).build());

        setItem(GRINDSTONE_RECIPES_SLOT, new ItemBuilder(Material.GRINDSTONE)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.hub.grindstone_recipes_name"), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.hub.count_defined", "count", grindstoneRecipeManager.count()), NamedTextColor.GRAY)).build());

        setItem(CARTOGRAPHY_RECIPES_SLOT, new ItemBuilder(Material.CARTOGRAPHY_TABLE)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.hub.cartography_recipes_name"), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.hub.count_defined", "count", cartographyRecipeManager.count()), NamedTextColor.GRAY)).build());

        setItem(LOOM_RECIPES_SLOT, new ItemBuilder(Material.LOOM)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.hub.loom_recipes_name"), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.hub.count_defined", "count", loomRecipeManager.count()), NamedTextColor.GRAY)).build());

        setItem(VILLAGER_TRADES_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.hub.villager_trades_name"), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.hub.count_defined_masc", "count", villagerTradeManager.count()), NamedTextColor.GRAY),
                        ComponentUtils.parseWithDefault(lang.raw("gui.hub.villager_bind_hint"), NamedTextColor.DARK_GRAY))
                .build());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.hub.close")));
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
        } else if (slot == GRINDSTONE_RECIPES_SLOT) {
            new GrindstoneRecipeBrowserGUI(player, grindstoneRecipeManager, chatPromptManager, this::reopen).open();
        } else if (slot == CARTOGRAPHY_RECIPES_SLOT) {
            new CartographyRecipeBrowserGUI(player, cartographyRecipeManager, chatPromptManager, this::reopen).open();
        } else if (slot == LOOM_RECIPES_SLOT) {
            new LoomRecipeBrowserGUI(player, loomRecipeManager, chatPromptManager, this::reopen).open();
        } else if (slot == VILLAGER_TRADES_SLOT) {
            new VillagerTradeBrowserGUI(player, villagerTradeManager, chatPromptManager, this::reopen).open();
        } else if (slot == CLOSE_SLOT) {
            close();
        }
    }

    private void reopen() {
        open();
    }

}
