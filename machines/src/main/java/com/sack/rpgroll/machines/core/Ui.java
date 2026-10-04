package com.sack.rpgroll.machines.core;

import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;

/** Botones de menú y formatos de número que comparten las tres máquinas. */
public final class Ui {

    private static final String[] ROMAN = {"0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
            "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};

    private Ui() {
    }

    public static ItemStack button(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        meta.lore(lore.stream().map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE))
                .toList());
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack button(Material material, NamespacedKey model, Component name, List<Component> lore) {
        ItemStack item = button(material, name, lore);
        if (model != null) {
            ItemMeta meta = item.getItemMeta();
            meta.setItemModel(model);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack filler() {
        return button(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
    }

    public static Component text(String legacy) {
        return ComponentUtils.parse(legacy);
    }

    public static String roman(int level) {
        return level >= 0 && level < ROMAN.length ? ROMAN[level] : String.valueOf(level);
    }

    /** 1.5 → "×1.5", 2 → "×2". */
    public static String times(double value) {
        return "×" + number(value);
    }

    /** 0.125 → "12.5%". */
    public static String percent(double fraction) {
        return number(fraction * 100) + "%";
    }

    public static String number(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "");
    }

    /** "lingote_de_cobalto" / "minecraft:zombie" → "Lingote de cobalto" para cuando no hay nombre. */
    public static String humanize(String id) {
        String plain = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        plain = plain.replace('_', ' ').toLowerCase(Locale.ROOT);
        return plain.isEmpty() ? plain : Character.toUpperCase(plain.charAt(0)) + plain.substring(1);
    }

    public static NamespacedKey model(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return NamespacedKey.fromString(text.trim().toLowerCase(Locale.ROOT));
    }
}
