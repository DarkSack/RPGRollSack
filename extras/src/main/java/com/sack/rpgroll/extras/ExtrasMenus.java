package com.sack.rpgroll.extras;

import com.sack.rpgroll.gui.MenuSkin;

/**
 * Menús dibujados de Extras (warps): el skin que registra el plugin con {@code menus.pack} de
 * config.yml (vacío de fábrica = menús de cofre normales). El menú del servidor (YAML) lleva su
 * propio {@code background:} en su archivo.
 */
public final class ExtrasMenus {

    public static final String MODULE = "extras";

    private ExtrasMenus() {
    }

    public static MenuSkin skin() {
        return MenuSkin.get(MODULE);
    }
}
