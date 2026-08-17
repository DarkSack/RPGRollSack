package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SeasonRegionManager;
import com.sack.rpgroll.seasons.core.WorldEventManager;
import com.sack.rpgroll.seasons.event.WorldEventEngine;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Hub del Season Studio — enlaza a los 4 navegadores de contenido. */
public class SeasonsBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int CALENDARS_SLOT = 10;
    private static final int SEASONS_SLOT = 12;
    private static final int WORLD_EVENTS_SLOT = 14;
    private static final int REGIONS_SLOT = 16;
    private static final int CLOSE_SLOT = 40;

    private final CalendarManager calendarManager;
    private final SeasonManager seasonManager;
    private final WorldEventManager worldEventManager;
    private final WorldEventEngine engine;
    private final SeasonRegionManager regionManager;
    private final ChatPromptManager chatPromptManager;

    public SeasonsBrowserGUI(Player player, CalendarManager calendarManager, SeasonManager seasonManager,
            WorldEventManager worldEventManager, WorldEventEngine engine, SeasonRegionManager regionManager,
            ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("gui.studio_hub.title"), SIZE);
        this.calendarManager = calendarManager;
        this.seasonManager = seasonManager;
        this.worldEventManager = worldEventManager;
        this.engine = engine;
        this.regionManager = regionManager;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        LangManager lang = chatPromptManager.lang();

        setItem(CALENDARS_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("gui.studio_hub.calendars"))
                .setLore(lang.component("gui.studio_hub.calendars_lore", "count", calendarManager.count())).build());

        setItem(SEASONS_SLOT, new ItemBuilder(Material.SUNFLOWER)
                .setName(lang.component("gui.studio_hub.seasons"))
                .setLore(lang.component("gui.studio_hub.seasons_lore", "count", seasonManager.count())).build());

        setItem(WORLD_EVENTS_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(lang.component("gui.studio_hub.world_events"))
                .setLore(lang.component("gui.studio_hub.world_events_lore", "count", worldEventManager.count()))
                .build());

        setItem(REGIONS_SLOT, new ItemBuilder(Material.MAP)
                .setName(lang.component("gui.studio_hub.regions"))
                .setLore(lang.component("gui.studio_hub.regions_lore", "count", regionManager.count())).build());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == CALENDARS_SLOT) {
            new CalendarBrowserGUI(player, calendarManager, seasonManager, chatPromptManager).open();
            return;
        }

        if (slot == SEASONS_SLOT) {
            new SeasonBrowserGUI(player, seasonManager, worldEventManager, chatPromptManager).open();
            return;
        }

        if (slot == WORLD_EVENTS_SLOT) {
            new WorldEventBrowserGUI(player, worldEventManager, engine, chatPromptManager).open();
            return;
        }

        if (slot == REGIONS_SLOT) {
            new SeasonRegionBrowserGUI(player, regionManager, seasonManager, calendarManager, chatPromptManager)
                    .open();
            return;
        }

        if (slot == CLOSE_SLOT) {
            close();
        }
    }

}
