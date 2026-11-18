package com.sack.rpgroll.gui;

import com.sack.rpgroll.gui.util.ItemBuilder;

import org.bukkit.inventory.ItemStack;

/**
 * Menús dibujados del core: el skin que registra el Bootstrap con {@code menus.pack} de config.yml
 * (vacío de fábrica = menús de cofre normales).
 */
public final class CoreMenus {

    public static final String MODULE = "core";

    private CoreMenus() {
    }

    public static MenuSkin skin() {
        return MenuSkin.get(MODULE);
    }

    /** Relleno de los menús del core: invisible con fondo, el cristal gris de siempre sin él. */
    public static ItemStack filler() {
        return skin().filler(ItemBuilder.createFiller());
    }
}
