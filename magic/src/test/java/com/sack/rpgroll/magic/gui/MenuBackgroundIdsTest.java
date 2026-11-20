package com.sack.rpgroll.magic.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("libro_hechizos", SpellbookGUI.BACKGROUND);
        assertEquals("runas", RuneSocketGUI.BACKGROUND);
        assertEquals("magic", MagicMenus.MODULE);
    }
}
