package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.SeasonCalendar;
import com.sack.rpgroll.seasons.core.SeasonManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Estaciones agregadas en orden — el ciclo las recorre en el orden en que aparecen acá, sin reordenar in-place (quitar y volver a agregar). */
public class CalendarEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int NAME_SLOT = 10;
    private static final int DESCRIPTION_SLOT = 11;
    private static final int SEASONS_START = 18;
    private static final int SEASONS_MAX = 27;
    private static final int ADD_SLOT = 49;
    private static final int BACK_SLOT = 53;

    private final CalendarManager calendarManager;
    private final SeasonManager seasonManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private SeasonCalendar current;

    public CalendarEditorGUI(Player player, SeasonCalendar calendar, CalendarManager calendarManager,
            SeasonManager seasonManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.calendar_editor.title", "id", calendar.id()), SIZE);
        this.current = calendar;
        this.calendarManager = calendarManager;
        this.seasonManager = seasonManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(SeasonCalendar updated) {
        current = updated;
        calendarManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        LangManager lang = chatPromptManager.lang();

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.common.name_label", "name", current.displayName())).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.common.description_title"))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? lang.raw("gui.common.no_description")
                                : current.description()))
                .build());

        List<String> seasonIds = current.seasonIds();

        for (int i = 0; i < seasonIds.size() && i < SEASONS_MAX; i++) {

            String seasonId = seasonIds.get(i);
            String displayName = seasonManager.get(seasonId).map(season -> season.displayName()).orElse(seasonId);

            setItem(SEASONS_START + i, new ItemBuilder(Material.SUNFLOWER)
                    .setName(Component.text((i + 1) + ". " + displayName, NamedTextColor.GREEN))
                    .setLore(lang.component("gui.common.id_label", "id", seasonId),
                            lang.component("gui.common.shift_remove"))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.calendar_editor.add"))
                .setLore(lang.component("gui.calendar_editor.add_lore")).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.common.prompt_name"), value -> replace(
                    new SeasonCalendar(current.id(), value, current.description(), current.seasonIds())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.common.prompt_description"),
                    value -> replace(
                            new SeasonCalendar(current.id(), current.displayName(), value, current.seasonIds())));
            return;
        }

        if (slot >= SEASONS_START && slot < SEASONS_START + Math.min(current.seasonIds().size(), SEASONS_MAX)) {
            if (event.isShiftClick()) {
                List<String> seasonIds = new ArrayList<>(current.seasonIds());
                seasonIds.remove(slot - SEASONS_START);
                replace(new SeasonCalendar(current.id(), current.displayName(), current.description(), seasonIds));
            }
            return;
        }

        if (slot == ADD_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.calendar_editor.prompt_add"), value -> {

                String seasonId = value.trim().toLowerCase(java.util.Locale.ROOT);

                if (!seasonManager.exists(seasonId)) {
                    chatPromptManager.lang().send(player, "gui.calendar_editor.unknown_season");
                    return;
                }

                List<String> seasonIds = new ArrayList<>(current.seasonIds());
                seasonIds.add(seasonId);
                replace(new SeasonCalendar(current.id(), current.displayName(), current.description(), seasonIds));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
