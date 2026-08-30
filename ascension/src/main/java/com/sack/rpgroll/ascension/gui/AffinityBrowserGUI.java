package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.Affinity;
import com.sack.rpgroll.ascension.core.AffinityManager;
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

public class AffinityBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final AffinityManager manager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<Affinity> affinities;

    public AffinityBrowserGUI(Player player, AffinityManager manager, ChatPromptManager chatPromptManager,
            LangManager lang) {
        super(player, lang.component("gui.affinity.browser_title"), SIZE);
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
        this.affinities = List.copyOf(manager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < affinities.size() && i < 36; i++) {
            Affinity affinity = affinities.get(i);
            setItem(i, new ItemBuilder(Material.BLAZE_POWDER)
                    .setName(Component.text(affinity.id(), NamedTextColor.YELLOW))
                    .setLore(ComponentUtils.parse(affinity.displayName()),
                            lang.component("gui.common.click_to_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.affinity.create_new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < affinities.size() && slot < 36) {
            new AffinityEditorGUI(player, affinities.get(slot), manager, chatPromptManager, this::reopen, lang)
                    .open();
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
        chatPromptManager.prompt(player, "gui.affinity.prompt_new_id", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (manager.exists(id)) {
                lang.send(player, "gui.affinity.id_exists");
                reopen();
                return;
            }

            manager.save(new Affinity(id, id, null, List.of()));
            reopen();
        });
    }

    private void reopen() {
        this.affinities = List.copyOf(manager.getAll());
        open();
    }

}
