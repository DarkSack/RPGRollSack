package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Editor de stats del mob (health, damage, defense, speed, armor, crítico,
 * esquive, precisión...). Click izquierdo +1, shift+izquierdo +10, click
 * derecho -1, shift+derecho -10.
 */
public class MobStatsEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_CUSTOM_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final MobEditorSession session;
    private final Runnable onBack;
    private final List<String> statIds;

    public MobStatsEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, Component.text("Stats: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
        this.statIds = new ArrayList<>(session.statRegistry.all());
        this.statIds.sort(String::compareTo);
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < statIds.size() && i < 36; i++) {
            setItem(i, statItem(statIds.get(i), session.stats.getOrDefault(statIds.get(i), 0.0)));
        }

        setItem(ADD_CUSTOM_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar stat custom", NamedTextColor.GREEN))
                .setLore(Component.text("Click para escribir uno nuevo por chat", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private org.bukkit.inventory.ItemStack statItem(String statId, double value) {
        return new ItemBuilder(value > 0 ? Material.REDSTONE : Material.GUNPOWDER)
                .setName(Component.text(formatName(statId) + ": " + formatNumber(value), NamedTextColor.RED))
                .setLore(Component.text("Click: +1 · Shift-click: +10", NamedTextColor.GRAY),
                        Component.text("Click derecho: -1 · Shift-click derecho: -10", NamedTextColor.GRAY))
                .build();
    }

    private String formatName(String key) {

        String[] parts = key.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }

        return result.toString();
    }

    private String formatNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
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

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < statIds.size() && slot < 36) {
            adjustStat(statIds.get(slot), event.getClick());
            return;
        }

        if (slot == ADD_CUSTOM_SLOT) {
            promptCustomStat();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void adjustStat(String statId, ClickType click) {

        double delta = delta(click);
        if (delta == 0) {
            return;
        }

        double updated = session.stats.getOrDefault(statId, 0.0) + delta;

        if (updated == 0) {
            session.stats.remove(statId);
        } else {
            session.stats.put(statId, updated);
        }

        build();
    }

    private void promptCustomStat() {
        session.chatPromptManager.prompt(player, "Escribí: <stat> <valor> (ej. magic_damage 15):", value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 2) {
                player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                return;
            }

            try {
                String statId = parts[0].toLowerCase(Locale.ROOT);
                session.stats.put(statId, Double.parseDouble(parts[1]));
                session.statRegistry.register(statId);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Valor numérico inválido.", NamedTextColor.RED));
                return;
            }

            reopen();
        });
    }

    private void reopen() {
        this.statIds.clear();
        this.statIds.addAll(session.statRegistry.all());
        this.statIds.sort(String::compareTo);
        build();
    }

}
