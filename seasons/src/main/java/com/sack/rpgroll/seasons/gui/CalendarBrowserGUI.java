package com.sack.rpgroll.seasons.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.SeasonCalendar;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class CalendarBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final CalendarManager calendarManager;
    private final SeasonManager seasonManager;
    private final ChatPromptManager chatPromptManager;
    private List<SeasonCalendar> calendars;

    public CalendarBrowserGUI(Player player, CalendarManager calendarManager, SeasonManager seasonManager,
            ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("gui.calendar_browser.title"), SIZE);
        this.calendarManager = calendarManager;
        this.seasonManager = seasonManager;
        this.chatPromptManager = chatPromptManager;
        this.calendars = List.copyOf(calendarManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        LangManager lang = chatPromptManager.lang();

        for (int i = 0; i < calendars.size() && i < 36; i++) {

            SeasonCalendar calendar = calendars.get(i);

            setItem(i, new ItemBuilder(Material.CLOCK)
                    .setName(ComponentUtils.parse(calendar.displayName()))
                    .setLore(lang.component("gui.common.id_label", "id", calendar.id()),
                            lang.component("gui.calendar_browser.season_count", "count", calendar.seasonIds().size()),
                            lang.component("gui.common.click_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.calendar_browser.new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < calendars.size() && slot < 36) {
            new CalendarEditorGUI(player, calendars.get(slot), calendarManager, seasonManager, chatPromptManager,
                    this::reopen).open();
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.calendar_browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (calendarManager.exists(id)) {
                chatPromptManager.lang().send(player, "gui.calendar_browser.already_exists");
                reopen();
                return;
            }

            calendarManager.save(new com.sack.rpgroll.seasons.core.SeasonCalendar(id, id, "", List.of()));
            reopen();
        });
    }

    private void reopen() {
        this.calendars = List.copyOf(calendarManager.getAll());
        open();
    }

}
