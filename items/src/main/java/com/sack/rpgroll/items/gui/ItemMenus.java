package com.sack.rpgroll.items.gui;

import com.sack.rpgroll.gui.MenuSkin;
import com.sack.rpgroll.gui.util.ItemBuilder;

import org.bukkit.inventory.ItemStack;

/**
 * Menús dibujados de Items: el skin que registra el plugin con {@code menus.pack} de config.yml
 * (vacío de fábrica = menús de cofre normales).
 */
public final class ItemMenus {

    public static final String MODULE = "items";

    private ItemMenus() {
    }

    public static MenuSkin skin() {
        return MenuSkin.get(MODULE);
    }

    /** Relleno: invisible con fondo, el cristal gris de siempre sin él. */
    public static ItemStack filler() {
        return skin().filler(ItemBuilder.createFiller());
    }
}
