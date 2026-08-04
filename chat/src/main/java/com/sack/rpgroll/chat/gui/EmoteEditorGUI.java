package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.emote.EmoteDefinition;
import com.sack.rpgroll.chat.emote.EmoteManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class EmoteEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int TEMPLATE_SLOT = 10;
    private static final int TARGET_TEMPLATE_SLOT = 11;
    private static final int RADIUS_SLOT = 12;
    private static final int BACK_SLOT = 26;

    private final EmoteManager emoteManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private EmoteDefinition current;

    public EmoteEditorGUI(Player player, EmoteDefinition emote, EmoteManager emoteManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Emote: " + emote.id(), NamedTextColor.GOLD), SIZE);
        this.current = emote;
        this.emoteManager = emoteManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(EmoteDefinition updated) {
        current = updated;
        emoteManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(TEMPLATE_SLOT, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(Component.text("Mensaje (sin objetivo)", NamedTextColor.YELLOW))
                .setLore(LegacyComponentSerializer.legacyAmpersand().deserialize(current.template()),
                        Component.text("Usa {player} · Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(TARGET_TEMPLATE_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Mensaje (con objetivo)", NamedTextColor.YELLOW))
                .setLore(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        current.targetTemplate() == null || current.targetTemplate().isBlank()
                                ? "(no configurado)" : current.targetTemplate()),
                        Component.text("Usa {player} y {target} · Click para escribir uno nuevo",
                                NamedTextColor.GRAY))
                .build());

        setItem(RADIUS_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(Component.text("Radio: " + (current.radius() <= 0 ? "todo el mundo" : current.radius()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +10 · Click derecho: -10 (0 = todo el mundo)", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == TEMPLATE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo mensaje (usa {player}):",
                    value -> replace(new EmoteDefinition(current.id(), value, current.targetTemplate(),
                            current.radius())));
            return;
        }

        if (slot == TARGET_TEMPLATE_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo mensaje con objetivo (usa {player} y {target}):",
                    value -> replace(new EmoteDefinition(current.id(), current.template(), value, current.radius())));
            return;
        }

        if (slot == RADIUS_SLOT) {
            double delta = click == ClickType.RIGHT ? -10 : 10;
            replace(new EmoteDefinition(current.id(), current.template(), current.targetTemplate(),
                    Math.max(0, current.radius() + delta)));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
