package com.sack.rpgroll.recipes.gui;

import com.sack.rpgroll.gui.MenuSkin;

/**
 * Menús dibujados del recetario: el skin que registra el plugin con {@code menus.pack} de config.yml
 * (vacío de fábrica = menús de cofre normales).
 */
public final class RecipesMenus {

    public static final String MODULE = "recipes";

    private RecipesMenus() {
    }

    public static MenuSkin skin() {
        return MenuSkin.get(MODULE);
    }
}
