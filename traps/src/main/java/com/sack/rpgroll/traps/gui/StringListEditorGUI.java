package com.sack.rpgroll.traps.gui;

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
import java.util.function.Consumer;

/**
 * Editor genérico de una {@code List<String>} — reusado tanto para
 * Condiciones ("player.level >= 30") como para Cadena (ids de trampa). No
 * conoce el significado semántico de cada línea, solo agrega/quita texto
 * crudo y delega el guardado a {@code onSave}.
 */
public class StringListEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int LIST_SLOTS = 36;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final ChatPromptManager chatPromptManager;
    private final String promptKey;
    private final Consumer<List<String>> onSave;
    private final Runnable onBack;
    private List<String> values;

    public StringListEditorGUI(Player player, String title, List<String> initial, ChatPromptManager chatPromptManager,
            String promptKey, Consumer<List<String>> onSave, Runnable onBack) {
        super(player, Component.text(title, NamedTextColor.GOLD), SIZE);
        this.values = new ArrayList<>(initial);
        this.chatPromptManager = chatPromptManager;
        this.promptKey = promptKey;
        this.onSave = onSave;
        this.onBack = onBack;
    }

    private LangManager lang() {
        return chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < values.size() && i < LIST_SLOTS; i++) {
            setItem(i, new ItemBuilder(Material.PAPER)
                    .setName(Component.text(values.get(i), NamedTextColor.YELLOW))
                    .setLore(lang().component("gui.stringlist.shift_remove"))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang().component("gui.stringlist.add"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < values.size() && slot < LIST_SLOTS) {
            if (event.isShiftClick()) {
                List<String> updated = new ArrayList<>(values);
                updated.remove(slot);
                replace(updated);
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

    private void replace(List<String> updated) {
        this.values = updated;
        onSave.accept(List.copyOf(updated));
        build();
    }

    private void promptAdd() {
        chatPromptManager.prompt(player, promptKey, value -> {
            if (value == null || value.isBlank()) {
                return;
            }
            List<String> updated = new ArrayList<>(values);
            updated.add(value.trim());
            replace(updated);
        });
    }

}
