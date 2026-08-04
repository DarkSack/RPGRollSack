package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SeasonRegionManager;
import com.sack.rpgroll.seasons.core.WorldEventManager;
import com.sack.rpgroll.seasons.event.WorldEventEngine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
        super(player, Component.text("Season Studio", NamedTextColor.GOLD), SIZE);
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

        setItem(CALENDARS_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Calendarios", NamedTextColor.AQUA))
                .setLore(Component.text(calendarManager.count() + " definido(s)", NamedTextColor.GRAY)).build());

        setItem(SEASONS_SLOT, new ItemBuilder(Material.SUNFLOWER)
                .setName(Component.text("Estaciones", NamedTextColor.GREEN))
                .setLore(Component.text(seasonManager.count() + " definida(s)", NamedTextColor.GRAY)).build());

        setItem(WORLD_EVENTS_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Eventos Mundiales", NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text(worldEventManager.count() + " definido(s)", NamedTextColor.GRAY)).build());

        setItem(REGIONS_SLOT, new ItemBuilder(Material.MAP)
                .setName(Component.text("Regiones", NamedTextColor.GOLD))
                .setLore(Component.text(regionManager.count() + " definida(s)", NamedTextColor.GRAY)).build());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton("Cerrar"));
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
