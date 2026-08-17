package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.PrestigeLevel;
import com.sack.rpgroll.ascension.core.PrestigeManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class PrestigeBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final PrestigeManager manager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<PrestigeLevel> levels;

    public PrestigeBrowserGUI(Player player, PrestigeManager manager, ChatPromptManager chatPromptManager,
            LangManager lang) {
        super(player, lang.component("gui.prestige.browser_title"), SIZE);
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
        this.levels = List.copyOf(manager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < levels.size() && i < 36; i++) {
            PrestigeLevel level = levels.get(i);
            setItem(i, new ItemBuilder(Material.NETHER_STAR)
                    .setName(lang.component("gui.prestige.item_name", "id", level.id()))
                    .setLore(lang.component("gui.prestige.item_level_required", "level", level.requiredLevel()),
                            lang.component("gui.common.click_to_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.prestige.create_new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < levels.size() && slot < 36) {
            new PrestigeEditorGUI(player, levels.get(slot), manager, chatPromptManager, this::reopen, lang).open();
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
        chatPromptManager.prompt(player, "gui.prestige.prompt_new_id", value -> {

            String id = value.trim();

            if (manager.exists(id)) {
                lang.send(player, "gui.prestige.id_exists");
                reopen();
                return;
            }

            manager.save(new PrestigeLevel(id, 100, 0, List.of()));
            reopen();
        });
    }

    private void reopen() {
        this.levels = List.copyOf(manager.getAll());
        open();
    }

}
