package com.sack.rpgroll.pass.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("pase", PassGUI.BACKGROUND);
        assertEquals("pase_misiones", MissionsGUI.BACKGROUND);
        assertEquals("pase_diaria", DailyGUI.BACKGROUND);
        assertEquals("pass", PassMenus.MODULE);
    }
}
