package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

    public MobCustomDataEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, Component.text("Datos custom: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
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
                    .setName(Component.text(key + ": " + session.customData.get(key), NamedTextColor.AQUA))
                    .setLore(Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar dato", NamedTextColor.GREEN))
                .setLore(Component.text("Click para escribir: clave valor", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
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
        session.chatPromptManager.prompt(player, "Escribí: <clave> <valor>:", value -> {

            String[] parts = value.trim().split("\\s+", 2);

            if (parts.length != 2) {
                player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                return;
            }

            Map<String, String> updated = new LinkedHashMap<>(session.customData);
            updated.put(parts[0], parts[1]);
            session.customData = updated;

            build();
        });
    }

}
