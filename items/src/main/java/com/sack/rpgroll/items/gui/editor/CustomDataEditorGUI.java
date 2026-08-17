package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Pares clave-valor libres, expuestos vía la API para que otros addons lean estado propio. */
public class CustomDataEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final EditorSession session;
    private final LangManager lang;
    private final Runnable onBack;
    private List<String> keys;

    public CustomDataEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("editor.custom_data.title", "id", session.original.id()), SIZE);
        this.session = session;
        this.lang = session.chatPromptManager.lang();
        this.onBack = onBack;
        this.keys = new ArrayList<>(session.customData.keySet());
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

        for (int i = 0; i < keys.size() && i < 36; i++) {

            String key = keys.get(i);

            setItem(i, new ItemBuilder(Material.NAME_TAG)
                    .setName(Component.text(key + ": " + session.customData.get(key), NamedTextColor.AQUA))
                    .setLore(lang.component("editor.common.shift_click_remove"))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("editor.custom_data.add"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < keys.size() && slot < 36) {

            if (event.isShiftClick()) {
                session.customData.remove(keys.get(slot));
                keys = new ArrayList<>(session.customData.keySet());
                build();
            }
            return;
        }

        if (slot == ADD_SLOT) {
            promptAdd();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAdd() {
        session.chatPromptManager.prompt(player, lang.raw("editor.custom_data.prompt_add"), value -> {

            String[] parts = value.trim().split("\\s+", 2);

            if (parts.length != 2) {
                lang.send(player, "editor.common.invalid_format");
                return;
            }

            session.customData.put(parts[0], parts[1]);
            keys = new ArrayList<>(session.customData.keySet());
            build();
        });
    }

}
