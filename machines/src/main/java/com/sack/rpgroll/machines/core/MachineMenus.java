package com.sack.rpgroll.machines.core;

import com.sack.rpgroll.gui.MenuSkin;

import org.bukkit.inventory.ItemStack;

/**
 * Menús dibujados del horno y del generador de mobs: el skin que registra el plugin con {@code menus.pack}
 * de config.yml (vacío de fábrica = menús de cofre normales). La cantera y las máquinas de área siguen
 * con el {@code menu.pack} de su propio archivo.
 */
public final class MachineMenus {

    public static final String MODULE = "machines";

    private MachineMenus() {
    }

    public static MenuSkin skin() {
        return MenuSkin.get(MODULE);
    }

    /** Relleno: invisible con fondo, el cristal gris de siempre sin él. */
    public static ItemStack filler() {
        return skin().filler(Ui.filler());
    }
}
