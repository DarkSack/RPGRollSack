package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobWeakness;
import com.sack.rpgroll.mobs.core.MobWeaknessType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Editor de resistencias (% de reducción por elemento) y debilidades (% de daño extra). */
public class ResistancesEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final String[] ELEMENTS = {
            "fire", "water", "ice", "electric", "poison", "dark", "magic", "explosion", "arrow"
    };

    private static final int ADD_CUSTOM_RESISTANCE_SLOT = 17;

    private static final int WEAKNESS_START_SLOT = 18;
    private static final int ADD_WEAKNESS_SLOT = 35;

    private static final int BACK_SLOT = 44;

    private final MobEditorSession session;
    private final Runnable onBack;

    public ResistancesEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, Component.text("Resistencias: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < ELEMENTS.length; i++) {

            String element = ELEMENTS[i];
            double value = session.resistances.getOrDefault(element, 0.0);

            setItem(i, new ItemBuilder(value > 0 ? Material.SHIELD : Material.IRON_INGOT)
                    .setName(Component.text(capitalize(element) + ": " + formatNumber(value) + "%",
                            NamedTextColor.AQUA))
                    .setLore(Component.text("Click: +5 · Shift-click: +25", NamedTextColor.GRAY),
                            Component.text("Click derecho: -5 · Shift-click derecho: -25", NamedTextColor.GRAY))
                    .build());
        }

        setItem(ADD_CUSTOM_RESISTANCE_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Resistencia custom", NamedTextColor.GREEN))
                .setLore(Component.text("Click para escribir: elemento porcentaje", NamedTextColor.GRAY))
                .build());

        List<MobWeakness> weaknesses = session.weaknesses;

        for (int i = 0; i < weaknesses.size() && i < 17; i++) {

            MobWeakness weakness = weaknesses.get(i);

            setItem(WEAKNESS_START_SLOT + i, new ItemBuilder(Material.TNT)
                    .setName(Component.text(weakness.type() + " " + weakness.key() + ": +"
                            + formatNumber(weakness.extraDamagePercent()) + "%", NamedTextColor.RED))
                    .setLore(Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_WEAKNESS_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar debilidad", NamedTextColor.GREEN))
                .setLore(Component.text("Click para escribir: TIPO clave porcentaje", NamedTextColor.GRAY),
                        Component.text("ej. ELEMENT fire 50", NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String formatNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private double delta(ClickType click) {
        return switch (click) {
            case LEFT -> 5;
            case SHIFT_LEFT -> 25;
            case RIGHT -> -5;
            case SHIFT_RIGHT -> -25;
            default -> 0;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < ELEMENTS.length) {
            adjustResistance(ELEMENTS[slot], event.getClick());
            return;
        }

        if (slot == ADD_CUSTOM_RESISTANCE_SLOT) {
            promptCustomResistance();
            return;
        }

        if (slot >= WEAKNESS_START_SLOT && slot < WEAKNESS_START_SLOT + Math.min(session.weaknesses.size(), 17)) {
            if (event.isShiftClick()) {
                List<MobWeakness> updated = new ArrayList<>(session.weaknesses);
                updated.remove(slot - WEAKNESS_START_SLOT);
                session.weaknesses = updated;
                build();
            }
            return;
        }

        if (slot == ADD_WEAKNESS_SLOT) {
            promptAddWeakness();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void adjustResistance(String element, ClickType click) {

        double delta = delta(click);
        if (delta == 0) {
            return;
        }

        double updated = Math.max(0, session.resistances.getOrDefault(element, 0.0) + delta);

        if (updated == 0) {
            session.resistances.remove(element);
        } else {
            session.resistances.put(element, updated);
        }

        build();
    }

    private void promptCustomResistance() {
        session.chatPromptManager.prompt(player, "Escribí: <elemento> <porcentaje> (ej. bleed 30):", value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 2) {
                player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                return;
            }

            try {
                session.resistances.put(parts[0].toLowerCase(Locale.ROOT), Double.parseDouble(parts[1]));
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Valor numérico inválido.", NamedTextColor.RED));
                return;
            }

            build();
        });
    }

    private void promptAddWeakness() {
        session.chatPromptManager.prompt(player, "Escribí: TIPO clave porcentaje (ej. ELEMENT fire 50):", value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 3) {
                player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                return;
            }

            try {
                MobWeaknessType type = MobWeaknessType.valueOf(parts[0].toUpperCase(Locale.ROOT));
                double percent = Double.parseDouble(parts[2]);

                List<MobWeakness> updated = new ArrayList<>(session.weaknesses);
                updated.add(new MobWeakness(type, parts[1], percent));
                session.weaknesses = updated;
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Tipo o porcentaje inválido.", NamedTextColor.RED));
                return;
            }

            build();
        });
    }

}
