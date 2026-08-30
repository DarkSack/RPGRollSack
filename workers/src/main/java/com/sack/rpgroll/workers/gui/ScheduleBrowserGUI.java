package com.sack.rpgroll.workers.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.workers.core.schedule.Schedule;
import com.sack.rpgroll.workers.core.schedule.ScheduleManager;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class ScheduleBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final ScheduleManager scheduleManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Schedule> schedules;

    public ScheduleBrowserGUI(Player player, ScheduleManager scheduleManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.schedule.browser.title"), NamedTextColor.GOLD), SIZE);
        this.scheduleManager = scheduleManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.schedules = List.copyOf(scheduleManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < schedules.size() && i < 36; i++) {

            Schedule schedule = schedules.get(i);

            setItem(i, new ItemBuilder(Material.CLOCK)
                    .setName(ComponentUtils.parse(schedule.displayName()))
                    .setLore(Component.text(chatPromptManager.lang().raw("gui.profession.browser.id_label", "id",
                            schedule.id()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.schedule.browser.entries_label", "count",
                                    schedule.entries().size()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.common.click_to_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(chatPromptManager.lang().raw("gui.schedule.browser.new"), NamedTextColor.GREEN))
                .build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < schedules.size() && slot < 36) {
            new ScheduleEditorGUI(player, schedules.get(slot), scheduleManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.schedule.browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (scheduleManager.exists(id)) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.schedule.browser.duplicate_id"),
                        NamedTextColor.RED));
                reopen();
                return;
            }

            scheduleManager.save(new Schedule(id, id, "", List.of()));
            reopen();
        });
    }

    private void reopen() {
        this.schedules = List.copyOf(scheduleManager.getAll());
        open();
    }

}
