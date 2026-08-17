package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.Achievement;
import com.sack.rpgroll.ascension.deferred.AchievementManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class AchievementEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 10;
    private static final int DESCRIPTION_SLOT = 16;
    private static final int BACK_SLOT = 26;

    private final AchievementManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private Achievement current;

    public AchievementEditorGUI(Player player, Achievement achievement, AchievementManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack, LangManager lang) {
        super(player, lang.component("gui.achievement.editor_title", "id", achievement.id()), SIZE);
        this.current = achievement;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = lang;
    }

    private void replace(Achievement updated) {
        current = updated;
        manager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.common.name_label", "name", current.displayName()))
                .setLore(lang.component("gui.common.click_new_value"))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.achievement.description_label"))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? lang.raw("gui.achievement.description_empty")
                                : current.description()))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "gui.achievement.prompt_new_name",
                    value -> replace(new Achievement(current.id(), value, current.description())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "gui.achievement.prompt_description",
                    value -> replace(new Achievement(current.id(), current.displayName(), value)));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
