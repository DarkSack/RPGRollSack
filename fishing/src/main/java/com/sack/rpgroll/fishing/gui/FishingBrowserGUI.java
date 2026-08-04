package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.fishing.core.BaitManager;
import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.fishing.core.FishingRegionManager;
import com.sack.rpgroll.fishing.core.FishingRodManager;
import com.sack.rpgroll.fishing.core.JunkManager;
import com.sack.rpgroll.fishing.core.TreasureManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Hub del Fishing Studio — enlaza a los 6 navegadores de contenido. */
public class FishingBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int SPECIES_SLOT = 10;
    private static final int RODS_SLOT = 12;
    private static final int BAITS_SLOT = 14;
    private static final int TREASURES_SLOT = 16;
    private static final int JUNK_SLOT = 28;
    private static final int REGIONS_SLOT = 30;
    private static final int CLOSE_SLOT = 40;

    private final FishSpeciesManager speciesManager;
    private final FishingRodManager rodManager;
    private final BaitManager baitManager;
    private final TreasureManager treasureManager;
    private final JunkManager junkManager;
    private final FishingRegionManager regionManager;
    private final ChatPromptManager chatPromptManager;

    public FishingBrowserGUI(Player player, FishSpeciesManager speciesManager, FishingRodManager rodManager,
            BaitManager baitManager, TreasureManager treasureManager, JunkManager junkManager,
            FishingRegionManager regionManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Fishing Studio", NamedTextColor.GOLD), SIZE);
        this.speciesManager = speciesManager;
        this.rodManager = rodManager;
        this.baitManager = baitManager;
        this.treasureManager = treasureManager;
        this.junkManager = junkManager;
        this.regionManager = regionManager;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(SPECIES_SLOT, new ItemBuilder(Material.TROPICAL_FISH)
                .setName(Component.text("Especies", NamedTextColor.AQUA))
                .setLore(Component.text(speciesManager.count() + " definida(s)", NamedTextColor.GRAY)).build());

        setItem(RODS_SLOT, new ItemBuilder(Material.FISHING_ROD)
                .setName(Component.text("Cañas", NamedTextColor.YELLOW))
                .setLore(Component.text(rodManager.count() + " definida(s)", NamedTextColor.GRAY)).build());

        setItem(BAITS_SLOT, new ItemBuilder(Material.STRING)
                .setName(Component.text("Carnadas", NamedTextColor.GREEN))
                .setLore(Component.text(baitManager.count() + " definida(s)", NamedTextColor.GRAY)).build());

        setItem(TREASURES_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text("Tesoros", NamedTextColor.GOLD))
                .setLore(Component.text(treasureManager.count() + " definido(s)", NamedTextColor.GRAY)).build());

        setItem(JUNK_SLOT, new ItemBuilder(Material.LEATHER_BOOTS)
                .setName(Component.text("Basura", NamedTextColor.GRAY))
                .setLore(Component.text(junkManager.count() + " definida(s)", NamedTextColor.GRAY)).build());

        setItem(REGIONS_SLOT, new ItemBuilder(Material.MAP)
                .setName(Component.text("Regiones de pesca", NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text(regionManager.count() + " definida(s)", NamedTextColor.GRAY)).build());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == SPECIES_SLOT) {
            new SpeciesBrowserGUI(player, speciesManager, baitManager, chatPromptManager).open();
            return;
        }

        if (slot == RODS_SLOT) {
            new RodBrowserGUI(player, rodManager, chatPromptManager).open();
            return;
        }

        if (slot == BAITS_SLOT) {
            new BaitBrowserGUI(player, baitManager, chatPromptManager).open();
            return;
        }

        if (slot == TREASURES_SLOT) {
            new TreasureBrowserGUI(player, treasureManager, chatPromptManager).open();
            return;
        }

        if (slot == JUNK_SLOT) {
            new JunkBrowserGUI(player, junkManager, chatPromptManager).open();
            return;
        }

        if (slot == REGIONS_SLOT) {
            new FishingRegionBrowserGUI(player, regionManager, chatPromptManager).open();
            return;
        }

        if (slot == CLOSE_SLOT) {
            close();
        }
    }

}
