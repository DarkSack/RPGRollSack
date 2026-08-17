package com.sack.rpgroll.npcs.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.npcs.core.NpcMenuDefinition;
import com.sack.rpgroll.npcs.core.NpcMenuManager;
import com.sack.rpgroll.npcs.listener.ChatPromptManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class NpcMenuBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final NpcMenuManager menuManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager langManager;
    private List<NpcMenuDefinition> menus;

    public NpcMenuBrowserGUI(Player player, NpcMenuManager menuManager, ChatPromptManager chatPromptManager,
            LangManager langManager) {
        super(player, langManager.component("menu.browser.title"), SIZE);
        this.menuManager = menuManager;
        this.chatPromptManager = chatPromptManager;
        this.langManager = langManager;
        this.menus = List.copyOf(menuManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < menus.size() && i < 36; i++) {

            NpcMenuDefinition menu = menus.get(i);

            setItem(i, new ItemBuilder(Material.CHEST)
                    .setName(Component.text(menu.id(), NamedTextColor.YELLOW))
                    .setLore(langManager.component("menu.browser.item_lore",
                            "items", menu.items().size(), "rows", menu.rows()),
                            langManager.component("menu.browser.click_to_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(langManager.component("menu.browser.create_new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(langManager.raw("menu.browser.close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < menus.size() && slot < 36) {
            new NpcMenuEditorGUI(player, menus.get(slot), menuManager, chatPromptManager, langManager,
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
        chatPromptManager.prompt(player, langManager.raw("menu.browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (menuManager.exists(id)) {
                langManager.send(player, "menu.browser.already_exists");
                reopen();
                return;
            }

            NpcMenuDefinition menu = new NpcMenuDefinition(id, "&8" + id, 3, List.of());
            menuManager.save(menu);
            reopen();
        });
    }

    private void reopen() {
        this.menus = List.copyOf(menuManager.getAll());
        open();
    }

}
