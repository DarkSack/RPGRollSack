package com.sack.rpgroll.ranching.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("mis_animales", MyAnimalsGUI.BACKGROUND);
        assertEquals("animal", AnimalDetailGUI.BACKGROUND);
        assertEquals("mercado_animales", AnimalMarketGUI.BACKGROUND);
        assertEquals("subastas_confirmar", ConfirmGUI.BACKGROUND);
        assertEquals("ranching", RanchMenus.MODULE);
    }
}
