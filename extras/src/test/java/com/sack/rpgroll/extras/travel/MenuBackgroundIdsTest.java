package com.sack.rpgroll.extras.travel;

import com.sack.rpgroll.extras.ExtrasMenus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("warps", WarpsGUI.BACKGROUND);
        assertEquals("extras", ExtrasMenus.MODULE);
    }
}
