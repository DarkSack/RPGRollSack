package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SeasonRegion;
import com.sack.rpgroll.seasons.core.SeasonRegionManager;
import com.sack.rpgroll.seasons.core.SeasonRegionOverrideMode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class SeasonRegionBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final SeasonRegionManager regionManager;
    private final SeasonManager seasonManager;
    private final CalendarManager calendarManager;
    private final ChatPromptManager chatPromptManager;
    private List<SeasonRegion> regions;

    public SeasonRegionBrowserGUI(Player player, SeasonRegionManager regionManager, SeasonManager seasonManager,
            CalendarManager calendarManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Regiones de Temporada", NamedTextColor.GOLD), SIZE);
        this.regionManager = regionManager;
        this.seasonManager = seasonManager;
        this.calendarManager = calendarManager;
        this.chatPromptManager = chatPromptManager;
        this.regions = List.copyOf(regionManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < regions.size() && i < 36; i++) {

            SeasonRegion region = regions.get(i);

            setItem(i, new ItemBuilder(Material.MAP)
                    .setName(Component.text(region.id(), NamedTextColor.GOLD))
                    .setLore(Component.text("Mundo: " + region.world(), NamedTextColor.GRAY),
                            Component.text("Modo: " + region.overrideMode(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear región nueva (en tu ubicación)", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < regions.size() && slot < 36) {
            new SeasonRegionEditorGUI(player, regions.get(slot), regionManager, seasonManager, calendarManager,
                    chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "Escribí el id de la nueva región:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (regionManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una región con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            var loc = player.getLocation();

            SeasonRegion region = new SeasonRegion(id, loc.getWorld().getName(),
                    loc.getBlockX() - 10, 0, loc.getBlockZ() - 10,
                    loc.getBlockX() + 10, 255, loc.getBlockZ() + 10,
                    SeasonRegionOverrideMode.FOLLOW_WORLD_CALENDAR, null, null);

            regionManager.save(region);
            reopen();
        });
    }

    private void reopen() {
        this.regions = List.copyOf(regionManager.getAll());
        open();
    }

}
