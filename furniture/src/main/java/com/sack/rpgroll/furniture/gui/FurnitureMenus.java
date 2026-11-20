package com.sack.rpgroll.furniture.gui;

import com.sack.rpgroll.gui.MenuSkin;
import com.sack.rpgroll.gui.util.ItemBuilder;

import org.bukkit.inventory.ItemStack;

/**
 * Menús dibujados de Furniture: el skin que registra el plugin con {@code menus.pack} de config.yml
 * (vacío de fábrica = menús de cofre normales).
 */
public final class FurnitureMenus {

    public static final String MODULE = "furniture";

    private FurnitureMenus() {
    }

    public static MenuSkin skin() {
        return MenuSkin.get(MODULE);
    }

    /** Relleno: invisible con fondo, el cristal gris de siempre sin él. */
    public static ItemStack filler() {
        return skin().filler(ItemBuilder.createFiller());
    }
}
