package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
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
 * Editor de stats RPGRoll (virtuales, agregadas por el motor de stats) y
 * atributos vanilla reales (visibles en el tooltip nativo de Minecraft).
 * Click izquierdo +1, shift+izquierdo +10, click derecho -1, shift+derecho
 * -10 — sin necesidad de tipear números para los ajustes más comunes.
 */
public class StatsEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final String[] VANILLA_ATTRIBUTES = {
            "MAX_HEALTH", "ATTACK_DAMAGE", "ATTACK_SPEED", "MOVEMENT_SPEED",
            "ARMOR", "ARMOR_TOUGHNESS", "KNOCKBACK_RESISTANCE", "LUCK"
    };

    private static final int ADD_CUSTOM_STAT_SLOT = 27;
    private static final int ATTRIBUTES_START_SLOT = 36;
    private static final int ADD_CUSTOM_ATTRIBUTE_SLOT = 44;
    private static final int BACK_SLOT = 49;

    private final EditorSession session;
    private final LangManager lang;
    private final Runnable onBack;
    private final List<String> statIds;

    public StatsEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("editor.stats.title", "id", session.original.id()), SIZE);
        this.session = session;
        this.lang = session.chatPromptManager.lang();
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

        // Fila 27-35 separa "stats custom" de "atributos vanilla"; 45-53 es la
        // fila de control (solo Volver acá).
        for (int slot = 27; slot <= 35; slot++) {
            setItem(slot, new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
                    .setName(lang.component("editor.stats.vanilla_attributes_label")).build());
        }
        for (int slot = 45; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY)).build());
        }

        for (int i = 0; i < statIds.size() && i < 27; i++) {
            setItem(i, statItem(statIds.get(i), session.stats.getOrDefault(statIds.get(i), 0.0)));
        }

        setItem(ADD_CUSTOM_STAT_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("editor.stats.add_custom_stat"))
                .setLore(lang.component("editor.stats.click_new_chat"))
                .build());

        for (int i = 0; i < VANILLA_ATTRIBUTES.length; i++) {

            String attribute = VANILLA_ATTRIBUTES[i];
            setItem(ATTRIBUTES_START_SLOT + i,
                    attributeItem(attribute, session.attributeModifiers.getOrDefault(attribute, 0.0)));
        }

        setItem(ADD_CUSTOM_ATTRIBUTE_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("editor.stats.add_custom_attribute"))
                .setLore(lang.component("editor.stats.click_new_chat"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.common.back")));
    }

    private org.bukkit.inventory.ItemStack statItem(String statId, double value) {
        return new ItemBuilder(value > 0 ? Material.REDSTONE : Material.GUNPOWDER)
                .setName(Component.text(formatName(statId) + ": " + formatNumber(value), NamedTextColor.RED))
                .setLore(lang.component("editor.stats.stat_hint1"), lang.component("editor.stats.stat_hint2"))
                .build();
    }

    private org.bukkit.inventory.ItemStack attributeItem(String attribute, double value) {
        return new ItemBuilder(value > 0 ? Material.DIAMOND : Material.FLINT)
                .setName(Component.text(formatName(attribute) + ": " + formatNumber(value), NamedTextColor.AQUA))
                .setLore(lang.component("editor.stats.attribute_hint1"), lang.component("editor.stats.attribute_hint2"))
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

        if (slot < statIds.size() && slot < 27) {
            adjustStat(statIds.get(slot), event.getClick());
            return;
        }

        if (slot == ADD_CUSTOM_STAT_SLOT) {
            promptCustomStat();
            return;
        }

        if (slot >= ATTRIBUTES_START_SLOT && slot < ATTRIBUTES_START_SLOT + VANILLA_ATTRIBUTES.length) {
            adjustAttribute(VANILLA_ATTRIBUTES[slot - ATTRIBUTES_START_SLOT], event.getClick());
            return;
        }

        if (slot == ADD_CUSTOM_ATTRIBUTE_SLOT) {
            promptCustomAttribute();
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

    private void adjustAttribute(String attribute, ClickType click) {

        double delta = delta(click);
        if (delta == 0) {
            return;
        }

        double updated = session.attributeModifiers.getOrDefault(attribute, 0.0) + delta;

        if (updated == 0) {
            session.attributeModifiers.remove(attribute);
        } else {
            session.attributeModifiers.put(attribute, updated);
        }

        build();
    }

    private void promptCustomStat() {
        session.chatPromptManager.prompt(player, lang.raw("editor.stats.prompt_custom_stat"), value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 2) {
                lang.send(player, "editor.common.invalid_format");
                return;
            }

            try {
                String statId = parts[0].toLowerCase(Locale.ROOT);
                session.stats.put(statId, Double.parseDouble(parts[1]));
                session.statRegistry.register(statId);
            } catch (NumberFormatException e) {
                lang.send(player, "editor.stats.invalid_value");
                return;
            }

            reopen();
        });
    }

    private void promptCustomAttribute() {
        session.chatPromptManager.prompt(player, lang.raw("editor.stats.prompt_custom_attribute"), value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 2) {
                lang.send(player, "editor.common.invalid_format");
                return;
            }

            try {
                session.attributeModifiers.put(parts[0].toUpperCase(Locale.ROOT),
                        Double.parseDouble(parts[1]));
            } catch (NumberFormatException e) {
                lang.send(player, "editor.stats.invalid_value");
                return;
            }

            build();
        });
    }

    private void reopen() {
        this.statIds.clear();
        this.statIds.addAll(session.statRegistry.all());
        this.statIds.sort(String::compareTo);
        open();
    }

}
