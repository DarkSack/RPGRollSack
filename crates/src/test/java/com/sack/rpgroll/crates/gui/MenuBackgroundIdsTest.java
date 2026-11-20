package com.sack.rpgroll.crates.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("cofre_premio", CrateSpinGUI.BACKGROUND);
        assertEquals("crates", CrateMenus.MODULE);
    }
}
