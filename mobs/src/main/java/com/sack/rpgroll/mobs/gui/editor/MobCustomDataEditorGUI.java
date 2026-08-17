package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pares clave-valor libres para que otros addons lean datos propios del mob. */
public class MobCustomDataEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final MobEditorSession session;
    private final Runnable onBack;
    private final LangManager lang;

    public MobCustomDataEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("gui.custom_data.title", "id",
                session.original.id()), SIZE);
        this.session = session;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<String> keys = new ArrayList<>(session.customData.keySet());

        for (int i = 0; i < keys.size() && i < 36; i++) {

            String key = keys.get(i);

            setItem(i, new ItemBuilder(Material.PAPER)
                    .setName(lang.component("gui.custom_data.entry_label", "key", key, "value",
                            session.customData.get(key)))
                    .setLore(lang.component("gui.common.shift_remove_dark"))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.custom_data.add"))
                .setLore(lang.component("gui.custom_data.add_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        List<String> keys = new ArrayList<>(session.customData.keySet());

        if (slot < keys.size() && slot < 36) {
            if (event.isShiftClick()) {
                Map<String, String> updated = new LinkedHashMap<>(session.customData);
                updated.remove(keys.get(slot));
                session.customData = updated;
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
        session.chatPromptManager.prompt(player, "gui.custom_data.prompt_add", value -> {

            String[] parts = value.trim().split("\\s+", 2);

            if (parts.length != 2) {
                lang.send(player, "gui.common.invalid_format");
                return;
            }

            Map<String, String> updated = new LinkedHashMap<>(session.customData);
            updated.put(parts[0], parts[1]);
            session.customData = updated;

            build();
        });
    }

}
