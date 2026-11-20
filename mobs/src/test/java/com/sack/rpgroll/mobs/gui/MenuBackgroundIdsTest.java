package com.sack.rpgroll.mobs.gui;

import com.sack.rpgroll.mobs.bestiary.BestiaryGUI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("bestiario", BestiaryGUI.BACKGROUND);
        assertEquals("mobs", MobMenus.MODULE);
    }
}
