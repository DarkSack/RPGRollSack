package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fishing.core.Junk;
import com.sack.rpgroll.fishing.core.JunkManager;
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

public class JunkBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final JunkManager junkManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<Junk> junks;

    public JunkBrowserGUI(Player player, JunkManager junkManager, ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("gui.junk.browser_title"), SIZE);
        this.junkManager = junkManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.junks = List.copyOf(junkManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < junks.size() && i < 36; i++) {

            Junk junk = junks.get(i);

            setItem(i, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(junk.icon()))
                    .setName(ComponentUtils.parse(junk.displayName()))
                    .setLore(lang.component("gui.common.id_label", "id", junk.id()),
                            lang.component("gui.common.click_to_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.junk.new_button"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < junks.size() && slot < 36) {
            new JunkEditorGUI(player, junks.get(slot), junkManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, lang.raw("gui.junk.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (junkManager.exists(id)) {
                lang.send(player, "gui.junk.already_exists");
                reopen();
                return;
            }

            junkManager.save(new Junk(id, id, "LEATHER_BOOTS", "", 1.0));
            reopen();
        });
    }

    private void reopen() {
        this.junks = List.copyOf(junkManager.getAll());
        open();
    }

}
