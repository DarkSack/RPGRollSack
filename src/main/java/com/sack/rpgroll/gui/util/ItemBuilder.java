package com.sack.rpgroll.gui.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Utilidad para crear ItemStacks para GUIs.
 */
public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    /**
     * Establece el nombre del item.
     */
    public ItemBuilder setName(Component name) {
        meta.displayName(name);
        return this;
    }

    /**
     * Establece el lore.
     */
    public ItemBuilder setLore(Component... lore) {
        meta.lore(Arrays.asList(lore));
        return this;
    }

    /**
     * Establece el lore.
     */
    public ItemBuilder setLore(List<Component> lore) {
        meta.lore(lore);
        return this;
    }

    /**
     * Construye el ItemStack.
     */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Item de relleno.
     */
    public static ItemStack createFiller() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName(Component.text(" ", NamedTextColor.GRAY))
                .build();
    }

    /**
     * Botón de confirmar.
     */
    public static ItemStack createConfirmButton(String text) {
        return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .setName(
                        Component.text("✔ " + text, NamedTextColor.GREEN)
                                .decorate(TextDecoration.BOLD))
                .build();
    }

    /**
     * Botón de cancelar.
     */
    public static ItemStack createCancelButton(String text) {
        return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setName(
                        Component.text("✖ " + text, NamedTextColor.RED)
                                .decorate(TextDecoration.BOLD))
                .build();
    }

}