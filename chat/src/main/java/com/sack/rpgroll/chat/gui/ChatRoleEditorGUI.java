package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.role.ChatRole;
import com.sack.rpgroll.chat.role.ChatRoleManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ChatRoleEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int PREFIX_SLOT = 9;
    private static final int SUFFIX_SLOT = 10;
    private static final int COLOR_SLOT = 11;
    private static final int ICON_SLOT = 12;
    private static final int PRIORITY_SLOT = 13;
    private static final int PERMISSION_SLOT = 14;
    private static final int BACK_SLOT = 26;

    private final ChatRoleManager roleManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private ChatRole current;

    public ChatRoleEditorGUI(Player player, ChatRole role, ChatRoleManager roleManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Rol: " + role.id(), NamedTextColor.GOLD), SIZE);
        this.current = role;
        this.roleManager = roleManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(ChatRole updated) {
        current = updated;
        roleManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(PREFIX_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse("Prefijo: " + current.prefix()).colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(SUFFIX_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse("Sufijo: " + current.suffix()).colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.WHITE_DYE)
                .setName(Component.text("Color: " + current.color(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir un color nuevo", NamedTextColor.GRAY))
                .build());

        setItem(ICON_SLOT, new ItemBuilder(resolveIcon())
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir un Material nuevo", NamedTextColor.GRAY))
                .build());

        setItem(PRIORITY_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(Component.text("Prioridad: " + current.priority(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY))
                .build());

        setItem(PERMISSION_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Permiso: " + (current.permission() == null || current.permission().isBlank()
                        ? "(ninguno)" : current.permission()), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo (vacío = todos)", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private Material resolveIcon() {
        try {
            return Material.valueOf(current.icon().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.PAPER;
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == PREFIX_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo prefijo (podés usar & para colores):",
                    value -> replace(new ChatRole(current.id(), value, current.suffix(), current.color(),
                            current.icon(), current.priority(), current.permission())));
            return;
        }

        if (slot == SUFFIX_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo sufijo (podés usar & para colores):",
                    value -> replace(new ChatRole(current.id(), current.prefix(), value, current.color(),
                            current.icon(), current.priority(), current.permission())));
            return;
        }

        if (slot == COLOR_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nombre del color (ej. GOLD):",
                    value -> replace(new ChatRole(current.id(), current.prefix(), current.suffix(),
                            value.trim().toUpperCase(java.util.Locale.ROOT), current.icon(), current.priority(),
                            current.permission())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nombre del Material (ej. DIAMOND):",
                    value -> replace(new ChatRole(current.id(), current.prefix(), current.suffix(), current.color(),
                            value.trim().toUpperCase(java.util.Locale.ROOT), current.priority(),
                            current.permission())));
            return;
        }

        if (slot == PRIORITY_SLOT) {
            int delta = click == ClickType.RIGHT ? -1 : 1;
            replace(new ChatRole(current.id(), current.prefix(), current.suffix(), current.color(), current.icon(),
                    current.priority() + delta, current.permission()));
            return;
        }

        if (slot == PERMISSION_SLOT) {
            chatPromptManager.prompt(player, "Escribí el permiso (vacío = sin restricción):",
                    value -> replace(new ChatRole(current.id(), current.prefix(), current.suffix(), current.color(),
                            current.icon(), current.priority(), value.trim())));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
