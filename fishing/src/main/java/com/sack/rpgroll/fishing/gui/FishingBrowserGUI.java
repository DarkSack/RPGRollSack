package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fishing.core.BaitManager;
import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.fishing.core.FishingRegionManager;
import com.sack.rpgroll.fishing.core.FishingRodManager;
import com.sack.rpgroll.fishing.core.JunkManager;
import com.sack.rpgroll.fishing.core.TreasureManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

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
    private final LangManager lang;

    public FishingBrowserGUI(Player player, FishSpeciesManager speciesManager, FishingRodManager rodManager,
            BaitManager baitManager, TreasureManager treasureManager, JunkManager junkManager,
            FishingRegionManager regionManager, ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("gui.studio.title"), SIZE);
        this.speciesManager = speciesManager;
        this.rodManager = rodManager;
        this.baitManager = baitManager;
        this.treasureManager = treasureManager;
        this.junkManager = junkManager;
        this.regionManager = regionManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(SPECIES_SLOT, new ItemBuilder(Material.TROPICAL_FISH)
                .setName(lang.component("gui.studio.species_name"))
                .setLore(lang.component("gui.studio.definida_lore", "count", speciesManager.count())).build());

        setItem(RODS_SLOT, new ItemBuilder(Material.FISHING_ROD)
                .setName(lang.component("gui.studio.rods_name"))
                .setLore(lang.component("gui.studio.definida_lore", "count", rodManager.count())).build());

        setItem(BAITS_SLOT, new ItemBuilder(Material.STRING)
                .setName(lang.component("gui.studio.baits_name"))
                .setLore(lang.component("gui.studio.definida_lore", "count", baitManager.count())).build());

        setItem(TREASURES_SLOT, new ItemBuilder(Material.CHEST)
                .setName(lang.component("gui.studio.treasures_name"))
                .setLore(lang.component("gui.studio.definido_lore", "count", treasureManager.count())).build());

        setItem(JUNK_SLOT, new ItemBuilder(Material.LEATHER_BOOTS)
                .setName(lang.component("gui.studio.junk_name"))
                .setLore(lang.component("gui.studio.definida_lore", "count", junkManager.count())).build());

        setItem(REGIONS_SLOT, new ItemBuilder(Material.MAP)
                .setName(lang.component("gui.studio.regions_name"))
                .setLore(lang.component("gui.studio.definida_lore", "count", regionManager.count())).build());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close")));
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
