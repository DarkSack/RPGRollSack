package com.sack.rpgroll.crafting.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("recetario_crafteo", RecipeBookGUI.BACKGROUND);
        assertEquals("crafting", CraftingMenus.MODULE);
    }
}
