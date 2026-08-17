package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.ItemAction;
import com.sack.rpgroll.items.core.ItemTrigger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista todos los triggers disponibles (RIGHT_CLICK, ENTITY_HIT,
 * BLOCK_BREAK, etc.) con la cantidad de acciones que cada uno ya tiene
 * configuradas — click abre el editor de acciones de ESE trigger.
 */
public class TriggersEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int BACK_SLOT = 44;

    private final EditorSession session;
    private final LangManager lang;
    private final Runnable onBack;
    private final ItemTrigger[] triggers = ItemTrigger.values();

    public TriggersEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("editor.triggers.title", "id", session.original.id()), SIZE);
        this.session = session;
        this.lang = session.chatPromptManager.lang();
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int slot = 36; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY)).build());
        }

        for (int i = 0; i < triggers.length && i < 36; i++) {

            ItemTrigger trigger = triggers[i];
            int count = session.triggers.getOrDefault(trigger, List.of()).size();

            setItem(i, new ItemBuilder(count > 0 ? Material.COMPARATOR : Material.LEVER)
                    .setName(Component.text(trigger.name(), count > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                    .setLore(lang.component("editor.triggers.action_count", "count", count))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < triggers.length && slot < 36) {

            ItemTrigger trigger = triggers[slot];
            List<ItemAction> actions = session.triggers.computeIfAbsent(trigger, t -> new ArrayList<>());

            new ActionListEditorGUI(player, lang.raw("editor.triggers.actions_of", "trigger", trigger.name()),
                    actions, session.chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void reopen() {
        new TriggersEditorGUI(player, session, onBack).open();
    }

}
