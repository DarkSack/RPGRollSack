package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.gui.MenuSkin;
import com.sack.rpgroll.gui.util.ItemBuilder;

import org.bukkit.inventory.ItemStack;

/**
 * Menús dibujados de Economy: el skin que registra el plugin con {@code menus.pack} de config.yml
 * (vacío de fábrica = menús de cofre normales).
 */
public final class EconomyMenus {

    public static final String MODULE = "economy";

    private EconomyMenus() {
    }

    public static MenuSkin skin() {
        return MenuSkin.get(MODULE);
    }

    /** Relleno: invisible con fondo, {@code plain} (el cristal de siempre) sin él. */
    public static ItemStack filler(ItemStack plain) {
        return skin().filler(plain);
    }

    public static ItemStack filler() {
        return filler(ItemBuilder.createFiller());
    }
}
