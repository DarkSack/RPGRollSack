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

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SeasonRegionEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int BOUNDS_SLOT = 10;
    private static final int CORNER_A_SLOT = 11;
    private static final int CORNER_B_SLOT = 12;
    private static final int MODE_SLOT = 13;
    private static final int PINNED_SEASON_SLOT = 14;
    private static final int PINNED_CALENDAR_SLOT = 15;
    private static final int BACK_SLOT = 40;

    private final SeasonRegionManager regionManager;
    private final SeasonManager seasonManager;
    private final CalendarManager calendarManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private SeasonRegion current;

    public SeasonRegionEditorGUI(Player player, SeasonRegion region, SeasonRegionManager regionManager,
            SeasonManager seasonManager, CalendarManager calendarManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Región: " + region.id(), NamedTextColor.GOLD), SIZE);
        this.current = region;
        this.regionManager = regionManager;
        this.seasonManager = seasonManager;
        this.calendarManager = calendarManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(SeasonRegion updated) {
        current = updated;
        regionManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(BOUNDS_SLOT, new ItemBuilder(Material.MAP)
                .setName(Component.text("Mundo: " + current.world(), NamedTextColor.YELLOW))
                .setLore(Component.text(String.format("(%.0f,%.0f,%.0f) a (%.0f,%.0f,%.0f)",
                        current.minX(), current.minY(), current.minZ(), current.maxX(), current.maxY(), current.maxZ()),
                        NamedTextColor.GRAY))
                .build());

        setItem(CORNER_A_SLOT, new ItemBuilder(Material.RED_CONCRETE)
                .setName(Component.text("Fijar esquina A (tu posición)", NamedTextColor.YELLOW))
                .build());

        setItem(CORNER_B_SLOT, new ItemBuilder(Material.BLUE_CONCRETE)
                .setName(Component.text("Fijar esquina B (tu posición)", NamedTextColor.YELLOW))
                .build());

        setItem(MODE_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(Component.text("Modo: " + current.overrideMode(), NamedTextColor.AQUA))
                .setLore(Component.text("Click para pasar al siguiente", NamedTextColor.GRAY)).build());

        setItem(PINNED_SEASON_SLOT, new ItemBuilder(Material.SUNFLOWER)
                .setName(Component.text(
                        "Estación fija: " + (current.pinnedSeasonId() == null ? "(ninguna)" : current.pinnedSeasonId()),
                        NamedTextColor.GREEN))
                .setLore(Component.text("Solo aplica en modo PINNED_SEASON", NamedTextColor.GRAY),
                        Component.text("Escribí el id de una estación (o 'ninguna')", NamedTextColor.GRAY))
                .build());

        setItem(PINNED_CALENDAR_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text(
                        "Calendario propio: " + (current.pinnedCalendarId() == null ? "(ninguno)" : current.pinnedCalendarId()),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Solo aplica en modo PINNED_CALENDAR", NamedTextColor.GRAY),
                        Component.text("Escribí el id de un calendario (o 'ninguno')", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == CORNER_A_SLOT) {
            Location loc = player.getLocation();
            replace(new SeasonRegion(current.id(), loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(),
                    loc.getBlockZ(), current.maxX(), current.maxY(), current.maxZ(), current.overrideMode(),
                    current.pinnedSeasonId(), current.pinnedCalendarId()));
            return;
        }

        if (slot == CORNER_B_SLOT) {
            Location loc = player.getLocation();
            replace(new SeasonRegion(current.id(), loc.getWorld().getName(), current.minX(), current.minY(),
                    current.minZ(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), current.overrideMode(),
                    current.pinnedSeasonId(), current.pinnedCalendarId()));
            return;
        }

        if (slot == MODE_SLOT) {
            SeasonRegionOverrideMode[] values = SeasonRegionOverrideMode.values();
            SeasonRegionOverrideMode next = values[(current.overrideMode().ordinal() + 1) % values.length];
            replace(new SeasonRegion(current.id(), current.world(), current.minX(), current.minY(), current.minZ(),
                    current.maxX(), current.maxY(), current.maxZ(), next, current.pinnedSeasonId(),
                    current.pinnedCalendarId()));
            return;
        }

        if (slot == PINNED_SEASON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id de la estación fija (o 'ninguna'):", value -> {

                String seasonId = value.equalsIgnoreCase("ninguna") ? null : value.trim().toLowerCase(java.util.Locale.ROOT);

                if (seasonId != null && !seasonManager.exists(seasonId)) {
                    player.sendMessage(Component.text("No existe esa estación.", NamedTextColor.RED));
                    return;
                }

                replace(new SeasonRegion(current.id(), current.world(), current.minX(), current.minY(),
                        current.minZ(), current.maxX(), current.maxY(), current.maxZ(), current.overrideMode(),
                        seasonId, current.pinnedCalendarId()));
            });
            return;
        }

        if (slot == PINNED_CALENDAR_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id del calendario propio (o 'ninguno'):", value -> {

                String calendarId = value.equalsIgnoreCase("ninguno") ? null : value.trim().toLowerCase(java.util.Locale.ROOT);

                if (calendarId != null && !calendarManager.exists(calendarId)) {
                    player.sendMessage(Component.text("No existe ese calendario.", NamedTextColor.RED));
                    return;
                }

                replace(new SeasonRegion(current.id(), current.world(), current.minX(), current.minY(),
                        current.minZ(), current.maxX(), current.maxY(), current.maxZ(), current.overrideMode(),
                        current.pinnedSeasonId(), calendarId));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
