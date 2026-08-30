package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.Title;
import com.sack.rpgroll.ascension.deferred.TitleManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class TitleBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final TitleManager manager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<Title> titles;

    public TitleBrowserGUI(Player player, TitleManager manager, ChatPromptManager chatPromptManager,
            LangManager lang) {
        super(player, lang.component("gui.title.browser_title"), SIZE);
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
        this.titles = List.copyOf(manager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < titles.size() && i < 36; i++) {
            Title title = titles.get(i);
            setItem(i, new ItemBuilder(Material.NAME_TAG)
                    .setName(Component.text(title.id(), NamedTextColor.YELLOW))
                    .setLore(ComponentUtils.parse(title.displayName()),
                            lang.component("gui.common.click_to_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.title.create_new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < titles.size() && slot < 36) {
            new TitleEditorGUI(player, titles.get(slot), manager, chatPromptManager, this::reopen, lang).open();
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
        chatPromptManager.prompt(player, "gui.title.prompt_new_id", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (manager.exists(id)) {
                lang.send(player, "gui.title.id_exists");
                reopen();
                return;
            }

            manager.save(new Title(id, id));
            reopen();
        });
    }

    private void reopen() {
        this.titles = List.copyOf(manager.getAll());
        open();
    }

}
