package com.sack.rpgroll.traps.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.traps.core.TrapDefinition;
import com.sack.rpgroll.traps.core.TrapManager;
import com.sack.rpgroll.traps.core.TrapTrigger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class TrapBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final TrapManager trapManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<TrapDefinition> traps;

    public TrapBrowserGUI(Player player, TrapManager trapManager, ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("gui.browser.title"), SIZE);
        this.trapManager = trapManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.traps = List.copyOf(trapManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < traps.size() && i < 36; i++) {

            TrapDefinition trap = traps.get(i);

            setItem(i, new ItemBuilder(trap.icon())
                    .setName(Component.text(trap.id(), NamedTextColor.YELLOW))
                    .setLore(lang.component("gui.browser.trigger", "value", trap.trigger().name()),
                            lang.component("gui.browser.click_to_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.browser.create_new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < traps.size() && slot < 36) {
            new TrapEditorHubGUI(player, traps.get(slot), trapManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "gui.browser.prompt_new_id", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (trapManager.exists(id)) {
                lang.send(player, "gui.browser.id_exists");
                reopen();
                return;
            }

            TrapDefinition trap = new TrapDefinition(id, id, "", Material.TRIPWIRE_HOOK, TrapTrigger.PRESSURE,
                    java.util.Map.of(), 1.5, List.of(), List.of(), 0, -1, List.of(), null, null);

            trapManager.save(trap);
            reopen();
        });
    }

    private void reopen() {
        this.traps = List.copyOf(trapManager.getAll());
        open();
    }

}
