package com.sack.rpgroll.sackresourcepack.gui.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Helper mínimo de items para las GUIs de SackResourcePack — sin relación con el ItemBuilder de :core. */
public final class ItemBuilder {

    private ItemBuilder() {
    }

    public static ItemStack of(Material material, String name, NamedTextColor color, String... lore) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));

        if (lore.length > 0) {

            List<Component> loreLines = new ArrayList<>();

            for (String line : lore) {
                loreLines.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(loreLines);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack filler() {

        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack backButton() {
        return of(Material.ARROW, "Volver", NamedTextColor.RED);
    }

}
