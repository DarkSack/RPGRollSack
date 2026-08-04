package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.ItemSkin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;

/** Editor de skins alternativas (apariencia sin afectar stats). */
public class SkinsEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final EditorSession session;
    private final Runnable onBack;

    public SkinsEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, Component.text("Skins: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
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

        for (int i = 0; i < session.skins.size() && i < 36; i++) {

            ItemSkin skin = session.skins.get(i);
            Material material = skin.material() != null ? parseMaterial(skin.material()) : Material.ARMOR_STAND;

            setItem(i, new ItemBuilder(material)
                    .setName(Component.text(skin.displayName(), NamedTextColor.AQUA))
                    .setLore(Component.text("id: " + skin.id(), NamedTextColor.GRAY),
                            Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar skin", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.ARMOR_STAND;
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < session.skins.size() && slot < 36) {
            if (event.isShiftClick()) {
                session.skins.remove(slot);
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
        session.chatPromptManager.prompt(player,
                "Escribí: <id> <nombre> <material> (ej. golden Dorada GOLDEN_SWORD):", value -> {

                    String[] parts = value.trim().split("\\s+");

                    if (parts.length < 3) {
                        player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                        return;
                    }

                    String id = parts[0];
                    String material = parts[parts.length - 1];
                    String displayName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1));

                    session.skins.add(new ItemSkin(id, displayName, material, null));
                    build();
                });
    }

}
