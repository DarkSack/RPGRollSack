package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.ItemAction;
import com.sack.rpgroll.items.gui.ChatPromptManager;

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

/**
 * Editor genérico de una lista de {@link ItemAction} — reutilizado tanto
 * por el editor de comportamiento (triggers) como por el de habilidades,
 * ya que ambos terminan en "una lista de acciones a ejecutar". Sintaxis de
 * alta: {@code TIPO clave=valor clave2=valor2} (ej. {@code PARTICLE
 * particle=FLAME count=10}), o solo {@code TIPO} para una acción sin
 * parámetros (ej. {@code HEAL}).
 */
public class ActionListEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final List<ItemAction> actions;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;

    public ActionListEditorGUI(Player player, String title, List<ItemAction> actions,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(title, NamedTextColor.GOLD), SIZE);
        this.actions = actions;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
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

        for (int i = 0; i < actions.size() && i < 36; i++) {

            ItemAction action = actions.get(i);

            List<Component> lore = new ArrayList<>();
            for (var entry : action.params().entrySet()) {
                lore.add(Component.text(entry.getKey() + "=" + entry.getValue(), NamedTextColor.GRAY));
            }
            lore.add(lang.component("editor.common.shift_click_remove"));

            setItem(i, new ItemBuilder(Material.COMMAND_BLOCK)
                    .setName(Component.text(action.type(), NamedTextColor.YELLOW))
                    .setLore(lore)
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("editor.action_list.add"))
                .setLore(lang.component("editor.action_list.add_hint1"),
                        lang.component("editor.action_list.add_hint2"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < actions.size() && slot < 36) {
            if (event.isShiftClick()) {
                actions.remove(slot);
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
        chatPromptManager.prompt(player, lang.raw("editor.action_list.prompt_add"), value -> {

            String[] tokens = value.trim().split("\\s+");

            if (tokens.length == 0 || tokens[0].isBlank()) {
                lang.send(player, "editor.common.invalid_format");
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

            actions.add(new ItemAction(type, params));
            build();
        });
    }

}
