package com.sack.rpgroll.workers.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.workers.core.schedule.Schedule;
import com.sack.rpgroll.workers.core.schedule.ScheduleActivity;
import com.sack.rpgroll.workers.core.schedule.ScheduleEntry;
import com.sack.rpgroll.workers.core.schedule.ScheduleManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScheduleEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int DESCRIPTION_SLOT = 11;
    private static final int ENTRIES_SLOT = 13;
    private static final int CLEAR_SLOT = 14;
    private static final int BACK_SLOT = 40;

    private final ScheduleManager scheduleManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Schedule current;

    public ScheduleEditorGUI(Player player, Schedule schedule, ScheduleManager scheduleManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.schedule.editor.title", "id", schedule.id()), NamedTextColor.GOLD), SIZE);
        this.current = schedule;
        this.scheduleManager = scheduleManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Schedule updated) {
        current = updated;
        scheduleManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.schedule.editor.name", "name",
                        current.displayName()), NamedTextColor.YELLOW)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.profession.editor.description"), NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank()
                        ? chatPromptManager.lang().raw("gui.editor.no_description") : current.description()))
                .build());

        setItem(ENTRIES_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.schedule.editor.entries", "count",
                        current.entries().size()), NamedTextColor.AQUA))
                .setLore(ItemBuilder.toLoreLines(chatPromptManager.lang().raw("gui.schedule.editor.entries_lore", "entries",
                        formatEntries(), "activities", java.util.Arrays.toString(ScheduleActivity.values()))))
                .build());

        setItem(CLEAR_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.schedule.editor.clear_entries"), NamedTextColor.RED))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    private String formatEntries() {

        if (current.entries().isEmpty()) {
            return chatPromptManager.lang().raw("gui.schedule.editor.no_entries");
        }

        StringBuilder builder = new StringBuilder();

        for (ScheduleEntry entry : current.entries()) {
            builder.append(entry.startTick()).append(" → ").append(entry.activity()).append("\n");
        }

        return builder.toString().strip();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_name"),
                    value -> replace(new Schedule(current.id(), value, current.description(), current.entries())));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_description"),
                    value -> replace(new Schedule(current.id(), current.displayName(), value, current.entries())));
        } else if (slot == ENTRIES_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.schedule.editor.prompt_entry"), this::addEntry);
        } else if (slot == CLEAR_SLOT) {
            replace(new Schedule(current.id(), current.displayName(), current.description(), List.of()));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void addEntry(String raw) {

        String[] parts = raw.split(";");

        if (parts.length < 2) {
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.schedule.editor.invalid_entry_format"), NamedTextColor.RED));
            build();
            return;
        }

        long tick;
        ScheduleActivity activity;

        try {
            tick = Long.parseLong(parts[0].trim());
            activity = ScheduleActivity.valueOf(parts[1].trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.schedule.editor.invalid_entry_values"), NamedTextColor.RED));
            build();
            return;
        }

        List<ScheduleEntry> entries = new ArrayList<>(current.entries());
        entries.add(new ScheduleEntry(tick, activity));

        replace(new Schedule(current.id(), current.displayName(), current.description(), entries));
    }

}
