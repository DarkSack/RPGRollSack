package com.sack.rpgroll.workers.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.workers.core.event.WorkerEventDefinition;
import com.sack.rpgroll.workers.core.event.WorkerEventManager;
import com.sack.rpgroll.workers.core.event.WorkerEventType;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class WorkerEventBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final WorkerEventManager eventManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<WorkerEventDefinition> events;

    public WorkerEventBrowserGUI(Player player, WorkerEventManager eventManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.event.browser.title"), NamedTextColor.GOLD), SIZE);
        this.eventManager = eventManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.events = List.copyOf(eventManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < events.size() && i < 36; i++) {

            WorkerEventDefinition event = events.get(i);

            setItem(i, new ItemBuilder(Material.PAPER)
                    .setName(ComponentUtils.parse(event.displayName()))
                    .setLore(Component.text(chatPromptManager.lang().raw("gui.profession.browser.id_label", "id",
                            event.id()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.event.browser.type_label", "type",
                                    event.type()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.common.click_to_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(chatPromptManager.lang().raw("gui.event.browser.new"), NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < events.size() && slot < 36) {
            new WorkerEventEditorGUI(player, events.get(slot), eventManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.event.browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (eventManager.exists(id)) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.event.browser.duplicate_id"),
                        NamedTextColor.RED));
                reopen();
                return;
            }

            eventManager.save(new WorkerEventDefinition(id, id, "", WorkerEventType.ILLNESS, 0, 6000, 0, 0, 0, 1.0));
            reopen();
        });
    }

    private void reopen() {
        this.events = List.copyOf(eventManager.getAll());
        open();
    }

}
