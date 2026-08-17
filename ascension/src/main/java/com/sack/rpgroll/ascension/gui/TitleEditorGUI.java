package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.Title;
import com.sack.rpgroll.ascension.deferred.TitleManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class TitleEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 11;
    private static final int BACK_SLOT = 26;

    private final TitleManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private Title current;

    public TitleEditorGUI(Player player, Title title, TitleManager manager, ChatPromptManager chatPromptManager,
            Runnable onBack, LangManager lang) {
        super(player, lang.component("gui.title.editor_title", "id", title.id()), SIZE);
        this.current = title;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = lang;
    }

    private void replace(Title updated) {
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

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "gui.title.prompt_new_name",
                    value -> replace(new Title(current.id(), value)));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
