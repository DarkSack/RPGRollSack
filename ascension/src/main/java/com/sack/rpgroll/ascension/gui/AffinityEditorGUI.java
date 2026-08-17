package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.Affinity;
import com.sack.rpgroll.ascension.core.AffinityManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class AffinityEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 10;
    private static final int OPPOSING_SLOT = 12;
    private static final int RESIST_SLOT = 14;
    private static final int BACK_SLOT = 26;

    private final AffinityManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private Affinity current;

    public AffinityEditorGUI(Player player, Affinity affinity, AffinityManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack, LangManager lang) {
        super(player, lang.component("gui.affinity.editor_title", "id", affinity.id()), SIZE);
        this.current = affinity;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = lang;
    }

    private void replace(Affinity updated) {
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

        setItem(OPPOSING_SLOT, new ItemBuilder(Material.SHIELD)
                .setName(lang.component("gui.affinity.opposing_label", "value", current.opposingId() == null
                        ? lang.raw("gui.affinity.opposing_none") : current.opposingId()))
                .setLore(lang.component("gui.affinity.prompt_opposing_hint"))
                .build());

        setItem(RESIST_SLOT, new ItemBuilder(Material.SHIELD)
                .setName(lang.component("gui.affinity.resist_label", "count", current.resistCauses().size()))
                .setLore(Component.text(String.join(", ", current.resistCauses()), NamedTextColor.GRAY),
                        lang.component("gui.affinity.resist_hint"),
                        lang.component("gui.affinity.resist_example"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "gui.affinity.prompt_new_name", value -> replace(
                    new Affinity(current.id(), value, current.opposingId(), current.resistCauses())));
            return;
        }

        if (slot == OPPOSING_SLOT) {
            chatPromptManager.prompt(player, "gui.affinity.prompt_opposing", value -> {
                String opposing = value.trim().equals("-") ? null : value.trim();
                replace(new Affinity(current.id(), current.displayName(), opposing, current.resistCauses()));
            });
            return;
        }

        if (slot == RESIST_SLOT) {
            chatPromptManager.prompt(player, "gui.affinity.prompt_resist",
                    value -> replace(new Affinity(current.id(), current.displayName(), current.opposingId(),
                            List.of(value.trim().toUpperCase().split(",")))));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
