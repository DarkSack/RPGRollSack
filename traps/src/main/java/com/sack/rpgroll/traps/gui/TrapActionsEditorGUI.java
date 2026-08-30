package com.sack.rpgroll.traps.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.traps.core.TrapAction;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Editor de {@code List<TrapAction>} — clon directo de
 * {@code DungeonActionListEditorGUI}. Sintaxis de alta:
 * {@code TIPO clave=valor clave2=valor2} (ej. {@code EXPLOSION power=2.5 break-blocks=false}).
 */
public class TrapActionsEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int LIST_SLOTS = 36;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final ChatPromptManager chatPromptManager;
    private final Consumer<List<TrapAction>> onSave;
    private final Runnable onBack;
    private List<TrapAction> actions;

    public TrapActionsEditorGUI(Player player, String title, List<TrapAction> initial,
            ChatPromptManager chatPromptManager, Consumer<List<TrapAction>> onSave, Runnable onBack) {
        super(player, Component.text(title, NamedTextColor.GOLD), SIZE);
        this.actions = new ArrayList<>(initial);
        this.chatPromptManager = chatPromptManager;
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

        for (int i = 0; i < actions.size() && i < LIST_SLOTS; i++) {

            TrapAction action = actions.get(i);

            List<Component> lore = new ArrayList<>();
            for (var entry : action.params().entrySet()) {
                lore.add(Component.text(entry.getKey() + "=" + entry.getValue(), NamedTextColor.GRAY));
            }
            lore.add(lang().component("gui.actionlist.shift_remove"));

            setItem(i, new ItemBuilder(Material.COMMAND_BLOCK)
                    .setName(Component.text(action.type(), NamedTextColor.YELLOW))
                    .setLore(lore)
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang().component("gui.actionlist.add"))
                .setLore(lang().component("gui.actionlist.add_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < actions.size() && slot < LIST_SLOTS) {
            if (event.isShiftClick()) {
                List<TrapAction> updated = new ArrayList<>(actions);
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

    private void replace(List<TrapAction> updated) {
        this.actions = updated;
        onSave.accept(List.copyOf(updated));
        build();
    }

    private void promptAdd() {
        chatPromptManager.prompt(player, "gui.actionlist.prompt_add", value -> {

            String[] tokens = value.trim().split("\\s+");

            if (tokens.length == 0 || tokens[0].isBlank()) {
                lang().send(player, "gui.actionlist.invalid_format");
                return;
            }

            String type = tokens[0].toUpperCase(Locale.ROOT);
            Map<String, String> params = new HashMap<>();

            for (int i = 1; i < tokens.length; i++) {
                String[] kv = tokens[i].split("=", 2);
                if (kv.length == 2) {
                    params.put(kv[0], kv[1]);
                }
            }

            List<TrapAction> updated = new ArrayList<>(actions);
            updated.add(new TrapAction(type, params));
            replace(updated);
        });
    }

}
