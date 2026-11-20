package com.sack.rpgroll.crates.gui;

import com.sack.rpgroll.gui.MenuSkin;
import com.sack.rpgroll.gui.util.ItemBuilder;

import org.bukkit.inventory.ItemStack;

/**
 * Menús dibujados de Crates: el skin que registra el plugin con {@code menus.pack} de config.yml
 * (vacío de fábrica = menús de cofre normales).
 */
public final class CrateMenus {

    public static final String MODULE = "crates";

    private CrateMenus() {
    }

    public static MenuSkin skin() {
        return MenuSkin.get(MODULE);
    }

    /** Relleno: invisible con fondo, el cristal gris de siempre sin él. */
    public static ItemStack filler() {
        return skin().filler(ItemBuilder.createFiller());
    }
}
