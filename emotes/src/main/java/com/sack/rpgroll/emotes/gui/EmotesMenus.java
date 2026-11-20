package com.sack.rpgroll.emotes.gui;

import com.sack.rpgroll.emotes.EmotesPlugin;
import com.sack.rpgroll.gui.MenuSkin;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Skin y relleno del menú de emotes: el skin que registra el plugin con {@code menus.pack} de config.yml
 * (vacío de fábrica = menús de cofre normales).
 */
final class EmotesMenus {

    private EmotesMenus() {
    }

    static MenuSkin skin() {
        return MenuSkin.get(EmotesPlugin.MENU_MODULE);
    }

    /** Relleno: invisible con fondo, el cristal gris de siempre sin él. */
    static ItemStack filler() {
        return skin().filler(ItemBuilder.createFiller());
    }

    /** Ítem sin la cursiva que el cliente pone por defecto a nombres y lores. */
    static ItemStack item(Material material, Component name, List<Component> lore) {
        return new ItemBuilder(material, 1)
                .setName(plain(name))
                .setLore(lore.stream().map(EmotesMenus::plain).toList())
                .build();
    }

    private static Component plain(Component c) {
        return c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
