package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SeasonRegion;
import com.sack.rpgroll.seasons.core.SeasonRegionManager;
import com.sack.rpgroll.seasons.core.SeasonRegionOverrideMode;

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
        super(player, chatPromptManager.lang().component("gui.region_editor.title", "id", region.id()), SIZE);
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

        LangManager lang = chatPromptManager.lang();

        setItem(BOUNDS_SLOT, new ItemBuilder(Material.MAP)
                .setName(lang.component("gui.common.world_label", "world", current.world()))
                .setLore(lang.component("gui.region_editor.bounds_lore",
                        "minX", String.format("%.0f", current.minX()),
                        "minY", String.format("%.0f", current.minY()),
                        "minZ", String.format("%.0f", current.minZ()),
                        "maxX", String.format("%.0f", current.maxX()),
                        "maxY", String.format("%.0f", current.maxY()),
                        "maxZ", String.format("%.0f", current.maxZ())))
                .build());

        setItem(CORNER_A_SLOT, new ItemBuilder(Material.RED_CONCRETE)
                .setName(lang.component("gui.region_editor.corner_a"))
                .build());

        setItem(CORNER_B_SLOT, new ItemBuilder(Material.BLUE_CONCRETE)
                .setName(lang.component("gui.region_editor.corner_b"))
                .build());

        setItem(MODE_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(lang.component("gui.common.mode_label", "mode", current.overrideMode()))
                .setLore(lang.component("gui.region_editor.mode_lore")).build());

        setItem(PINNED_SEASON_SLOT, new ItemBuilder(Material.SUNFLOWER)
                .setName(lang.component("gui.region_editor.pinned_season_label", "value",
                        current.pinnedSeasonId() == null ? lang.raw("gui.common.none_fem") : current.pinnedSeasonId()))
                .setLore(lang.component("gui.region_editor.pinned_season_lore_1"),
                        lang.component("gui.region_editor.pinned_season_lore_2"))
                .build());

        setItem(PINNED_CALENDAR_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("gui.region_editor.pinned_calendar_label", "value",
                        current.pinnedCalendarId() == null ? lang.raw("gui.common.none_masc")
                                : current.pinnedCalendarId()))
                .setLore(lang.component("gui.region_editor.pinned_calendar_lore_1"),
                        lang.component("gui.region_editor.pinned_calendar_lore_2"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
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
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.region_editor.prompt_pinned_season"), value -> {

                String seasonId = value.equalsIgnoreCase("ninguna") ? null : value.trim().toLowerCase(java.util.Locale.ROOT);

                if (seasonId != null && !seasonManager.exists(seasonId)) {
                    chatPromptManager.lang().send(player, "gui.region_editor.unknown_season");
                    return;
                }

                replace(new SeasonRegion(current.id(), current.world(), current.minX(), current.minY(),
                        current.minZ(), current.maxX(), current.maxY(), current.maxZ(), current.overrideMode(),
                        seasonId, current.pinnedCalendarId()));
            });
            return;
        }

        if (slot == PINNED_CALENDAR_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.region_editor.prompt_pinned_calendar"), value -> {

                String calendarId = value.equalsIgnoreCase("ninguno") ? null : value.trim().toLowerCase(java.util.Locale.ROOT);

                if (calendarId != null && !calendarManager.exists(calendarId)) {
                    chatPromptManager.lang().send(player, "gui.region_editor.unknown_calendar");
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
