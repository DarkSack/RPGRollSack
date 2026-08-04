package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobCategory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Editor de identidad: nombre, categoría, nivel, rareza, color, tags y descripción. */
public class IdentityEditorGUI extends InventoryGUI {

    private static final int SIZE = 36;

    private static final int NAME_SLOT = 10;
    private static final int NAME_COLOR_SLOT = 11;
    private static final int CATEGORY_SLOT = 12;
    private static final int LEVEL_SLOT = 13;
    private static final int RARITY_SLOT = 14;
    private static final int DESCRIPTION_SLOT = 15;

    private static final int TAGS_START_SLOT = 18;
    private static final int ADD_TAG_SLOT = 26;
    private static final int BACK_SLOT = 31;

    private final MobEditorSession session;
    private final Runnable onBack;

    public IdentityEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, Component.text("Identidad: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + session.displayName, NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo (admite &códigos)", NamedTextColor.GRAY))
                .build());

        setItem(NAME_COLOR_SLOT, new ItemBuilder(Material.WHITE_DYE)
                .setName(Component.text("Color de nombre: "
                        + (session.nameColor != null ? session.nameColor : "(por rareza)"), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: escribir (ej. gold o #FF8800)", NamedTextColor.GRAY),
                        Component.text("Shift-click: quitar (usar color de rareza)", NamedTextColor.GRAY))
                .build());

        setItem(CATEGORY_SLOT, new ItemBuilder(Material.ARMOR_STAND)
                .setName(Component.text("Categoría: " + session.category, NamedTextColor.YELLOW))
                .setLore(Component.text("Click para pasar a la siguiente", NamedTextColor.GRAY))
                .build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Nivel: " + session.level, NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Shift-click: +10", NamedTextColor.GRAY),
                        Component.text("Click derecho: -1 · Shift-click derecho: -10", NamedTextColor.GRAY))
                .build());

        setItem(RARITY_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Rareza: " + session.rarityId, NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir el id de rareza", NamedTextColor.GRAY))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(Component.text(session.description.isBlank() ? "(vacía)" : session.description,
                        NamedTextColor.GRAY))
                .build());

        for (int i = 0; i < session.tags.size() && i < 8; i++) {
            setItem(TAGS_START_SLOT + i, new ItemBuilder(Material.PAPER)
                    .setName(Component.text(session.tags.get(i), NamedTextColor.AQUA))
                    .setLore(Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_TAG_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar tag", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            session.chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> {
                session.displayName = value;
                build();
            });
            return;
        }

        if (slot == NAME_COLOR_SLOT) {
            if (event.isShiftClick()) {
                session.nameColor = null;
                build();
                return;
            }
            session.chatPromptManager.prompt(player, "Escribí un color (ej. gold, red, #FF8800):", value -> {
                session.nameColor = value.trim();
                build();
            });
            return;
        }

        if (slot == CATEGORY_SLOT) {
            MobCategory[] values = MobCategory.values();
            session.category = values[(session.category.ordinal() + 1) % values.length];
            build();
            return;
        }

        if (slot == LEVEL_SLOT) {
            session.level = Math.max(1, session.level + (int) delta(event.getClick()));
            build();
            return;
        }

        if (slot == RARITY_SLOT) {
            session.chatPromptManager.prompt(player, "Escribí el id de rareza:", value -> {
                session.rarityId = value.trim().toLowerCase();
                build();
            });
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            session.chatPromptManager.prompt(player, "Escribí la descripción:", value -> {
                session.description = value;
                build();
            });
            return;
        }

        if (slot >= TAGS_START_SLOT && slot < TAGS_START_SLOT + Math.min(session.tags.size(), 8)) {
            if (event.isShiftClick()) {
                session.tags.remove(slot - TAGS_START_SLOT);
                build();
            }
            return;
        }

        if (slot == ADD_TAG_SLOT) {
            session.chatPromptManager.prompt(player, "Escribí el nuevo tag:", value -> {
                List<String> updated = new ArrayList<>(session.tags);
                updated.add(value.trim());
                session.tags = updated;
                build();
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private double delta(ClickType click) {
        return switch (click) {
            case LEFT -> 1;
            case SHIFT_LEFT -> 10;
            case RIGHT -> -1;
            case SHIFT_RIGHT -> -10;
            default -> 0;
        };
    }

}
