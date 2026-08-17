package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fishing.core.Junk;
import com.sack.rpgroll.fishing.core.JunkManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class JunkEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int WEIGHT_SLOT = 13;
    private static final int BACK_SLOT = 40;

    private final JunkManager junkManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private Junk current;

    public JunkEditorGUI(Player player, Junk junk, JunkManager junkManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.junk.editor_title", "id", junk.id()), SIZE);
        this.current = junk;
        this.junkManager = junkManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onBack = onBack;
    }

    private void replace(Junk updated) {
        current = updated;
        junkManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.junk.field_name", "name", current.displayName())).build());

        setItem(ICON_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.icon()))
                .setName(lang.component("gui.junk.field_icon", "icon", current.icon())).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.common.description_title"))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? lang.raw("gui.common.no_description") : current.description()))
                .build());

        setItem(WEIGHT_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(lang.component("gui.junk.field_weight", "value", current.weight()))
                .setLore(lang.component("gui.common.plusminus_1")).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.junk.prompt_name"),
                    value -> replace(new Junk(current.id(), value, current.icon(), current.description(),
                            current.weight())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.junk.prompt_icon"),
                    value -> replace(new Junk(current.id(), current.displayName(), value, current.description(),
                            current.weight())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.junk.prompt_description"),
                    value -> replace(new Junk(current.id(), current.displayName(), current.icon(), value,
                            current.weight())));
            return;
        }

        if (slot == WEIGHT_SLOT) {
            replace(new Junk(current.id(), current.displayName(), current.icon(), current.description(),
                    Math.max(0.01, current.weight() + sign)));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
