package com.sack.rpgroll.items.gui;

import com.sack.rpgroll.items.accessory.AccessoryGUI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("accesorios", AccessoryGUI.BACKGROUND);
        assertEquals("items", ItemMenus.MODULE);
    }
}
