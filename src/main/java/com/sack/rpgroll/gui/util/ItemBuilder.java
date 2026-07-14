package com.sack.rpgroll.gui.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
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
    public ItemBuilder setName(String name) {
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        return this;
    }

    /**
     * Establece el lore del item.
     */
    public ItemBuilder setLore(String... lore) {
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(loreList);
        return this;
    }

    /**
     * Establece el lore del item desde una lista.
     */
    public ItemBuilder setLore(List<String> lore) {
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(loreList);
        return this;
    }

    /**
     * Construye el ItemStack final.
     */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Crea un item de relleno (panel de cristal gris).
     */
    public static ItemStack createFiller() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setName("&7")
                .build();
    }

    /**
     * Crea un botón de confirmar.
     */
    public static ItemStack createConfirmButton(String text) {
        return new ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .setName("&a&l✔ " + text)
                .build();
    }

    /**
     * Crea un botón de cancelar.
     */
    public static ItemStack createCancelButton(String text) {
        return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setName("&c&l✖ " + text)
                .build();
    }

}
