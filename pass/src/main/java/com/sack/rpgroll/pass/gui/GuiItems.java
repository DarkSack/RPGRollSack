package com.sack.rpgroll.pass.gui;

import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Ítems de menú sin la cursiva que el cliente pone por defecto a nombres y lores. */
final class GuiItems {

    private GuiItems() {
    }

    static ItemStack item(Material material, Component name, List<Component> lore) {
        return item(material, 1, name, lore);
    }

    static ItemStack item(Material material, int amount, Component name, List<Component> lore) {
        return new ItemBuilder(material, Math.max(1, Math.min(64, amount)))
                .setName(plain(name))
                .setLore(lore.stream().map(GuiItems::plain).toList())
                .build();
    }

    /** Barra de progreso de texto: ▮▮▮▮▯▯▯▯▯▯ */
    static String bar(int value, int max, int width) {

        int filled = max <= 0 ? width : (int) Math.round(Math.min(1.0, (double) value / max) * width);
        return "&a" + "▮".repeat(filled) + "&8" + "▮".repeat(width - filled);
    }

    private static Component plain(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

}
