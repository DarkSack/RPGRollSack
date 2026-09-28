package com.sack.rpgroll.recipes.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Botones y retoques de ítems de los menús del recetario. */
final class Buttons {

    private Buttons() {
    }

    /** Botón con nombre {@code <key>.name} y lore {@code <key>.lore} (si existe) del idioma. */
    static ItemStack of(LangManager lang, Material material, String key, Object... placeholders) {
        String loreKey = key + ".lore";
        String lore = lang.raw(loreKey, placeholders);
        return of(material, text(lang.raw(key + ".name", placeholders)),
                lore.equals(loreKey) ? List.of() : lines(lore));
    }

    static ItemStack of(Material material, Component name, List<Component> lore) {
        ItemStack stack = ItemStack.of(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }

    static ItemStack filler() {
        return of(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "), List.of());
    }

    /** Texto de menú: sin la cursiva que Minecraft pone por defecto a nombres y lore. */
    static Component text(String raw) {
        return ComponentUtils.parse(raw).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    static List<Component> lines(String raw) {
        List<Component> lines = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            for (String line : raw.split("\n")) {
                lines.add(text(line));
            }
        }
        return lines;
    }

    /** Copia del ítem con líneas añadidas al final de su lore (sin tocar el original). */
    static ItemStack withLore(ItemStack item, List<Component> extra) {
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null || extra.isEmpty()) {
            return copy;
        }
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.addAll(extra);
        meta.lore(lore);
        copy.setItemMeta(meta);
        return copy;
    }
}
