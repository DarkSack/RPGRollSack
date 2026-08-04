package com.sack.rpgroll.npcs.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.npcs.core.NpcAction;
import com.sack.rpgroll.npcs.core.NpcAction.NpcActionType;
import com.sack.rpgroll.npcs.core.NpcMenuDefinition;
import com.sack.rpgroll.npcs.core.NpcMenuItem;
import com.sack.rpgroll.npcs.core.NpcMenuManager;
import com.sack.rpgroll.npcs.listener.ChatPromptManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Editor de un menú de NPC — título/filas, y sus ítems clickeables. Cada
 * ítem se agrega vía una sintaxis compacta de una línea; las acciones de
 * cada ítem se agregan igual, una condición por vez.
 */
public class NpcMenuEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int TITLE_SLOT = 0;
    private static final int ROWS_SLOT = 1;
    private static final int ADD_ITEM_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final NpcMenuManager menuManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private NpcMenuDefinition current;

    public NpcMenuEditorGUI(Player player, NpcMenuDefinition definition, NpcMenuManager menuManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Menú: " + definition.id(), NamedTextColor.GOLD), SIZE);
        this.current = definition;
        this.menuManager = menuManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(NpcMenuDefinition updated) {
        current = updated;
        menuManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(TITLE_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(LegacyComponentSerializer.legacyAmpersand().deserialize("Título: " + current.title())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(ROWS_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text("Filas: " + current.rows(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1 (1-6)", NamedTextColor.GRAY))
                .build());

        List<NpcMenuItem> items = current.items();

        for (int i = 0; i < items.size() && i < 36; i++) {

            NpcMenuItem item = items.get(i);

            setItem(9 + i, new ItemBuilder(resolveMaterial(item.material()))
                    .setName(LegacyComponentSerializer.legacyAmpersand()
                            .deserialize(item.displayName().isBlank() ? item.material() : item.displayName())
                            .colorIfAbsent(NamedTextColor.WHITE))
                    .setLore(Component.text("Slot " + item.slot() + " · " + item.actions().size() + " acción(es)",
                            NamedTextColor.GRAY),
                            Component.text("Click para agregar una acción · Shift-click para quitar",
                                    NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_ITEM_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar ítem al menú", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private Material resolveMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.PAPER;
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == TITLE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo título del menú:",
                    value -> replace(new NpcMenuDefinition(current.id(), value, current.rows(), current.items())));
            return;
        }

        if (slot == ROWS_SLOT) {
            int delta = click == ClickType.RIGHT ? -1 : 1;
            replace(new NpcMenuDefinition(current.id(), current.title(), current.rows() + delta, current.items()));
            return;
        }

        if (slot >= 9 && slot < 9 + Math.min(current.items().size(), 36)) {

            int index = slot - 9;

            if (event.isShiftClick()) {
                List<NpcMenuItem> updated = new ArrayList<>(current.items());
                updated.remove(index);
                replace(new NpcMenuDefinition(current.id(), current.title(), current.rows(), updated));
                return;
            }

            promptAddAction(index);
            return;
        }

        if (slot == ADD_ITEM_SLOT) {
            promptAddItem();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAddItem() {
        chatPromptManager.prompt(player, "Escribí: slot;material;nombre (ej. 11;IRON_SWORD;&fEspada):", value -> {

            String[] parts = value.split(";", 3);

            if (parts.length < 2) {
                player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                reopen();
                return;
            }

            int slotNumber;
            try {
                slotNumber = Integer.parseInt(parts[0].trim());
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("El slot debe ser un número.", NamedTextColor.RED));
                reopen();
                return;
            }

            String material = parts[1].trim().toUpperCase(Locale.ROOT);
            String name = parts.length > 2 ? parts[2].trim() : "";

            List<NpcMenuItem> updated = new ArrayList<>(current.items());
            updated.add(new NpcMenuItem(slotNumber, material, name, List.of(), List.of()));

            replace(new NpcMenuDefinition(current.id(), current.title(), current.rows(), updated));
        });
    }

    private void promptAddAction(int itemIndex) {
        chatPromptManager.prompt(player, "Escribí: TIPO;valor (ej. GIVE_ITEM;SHIELD,1 — tipos: "
                + String.join(", ", java.util.Arrays.stream(NpcActionType.values()).map(Enum::name).toList()) + "):",
                value -> {

                    String[] parts = value.split(";", 2);

                    if (parts.length < 2) {
                        player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                        reopen();
                        return;
                    }

                    NpcActionType type;
                    try {
                        type = NpcActionType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Component.text("Tipo de acción inválido.", NamedTextColor.RED));
                        reopen();
                        return;
                    }

                    List<NpcMenuItem> items = new ArrayList<>(current.items());
                    NpcMenuItem item = items.get(itemIndex);

                    List<NpcAction> actions = new ArrayList<>(item.actions());
                    actions.add(new NpcAction(type, parts[1].trim()));

                    items.set(itemIndex, new NpcMenuItem(item.slot(), item.material(), item.displayName(),
                            item.lore(), actions));

                    replace(new NpcMenuDefinition(current.id(), current.title(), current.rows(), items));
                });
    }

    private void reopen() {
        new NpcMenuEditorGUI(player, current, menuManager, chatPromptManager, onBack).open();
    }

}
