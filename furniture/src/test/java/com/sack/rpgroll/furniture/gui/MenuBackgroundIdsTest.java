package com.sack.rpgroll.furniture.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("muebles", FurnitureMenu.BACKGROUND);
        assertEquals("furniture", FurnitureMenus.MODULE);
    }
}
