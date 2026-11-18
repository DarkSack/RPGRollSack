package com.sack.rpgroll.gui;

import com.sack.rpgroll.gui.character.ClassSelectionGUI;
import com.sack.rpgroll.gui.character.RaceSelectionGUI;
import com.sack.rpgroll.gui.confirm.ConfirmGUI;
import com.sack.rpgroll.gui.job.JobAbandonGUI;
import com.sack.rpgroll.gui.job.JobsGUI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("raza", RaceSelectionGUI.BACKGROUND);
        assertEquals("clase", ClassSelectionGUI.BACKGROUND);
        assertEquals("trabajos", JobsGUI.BACKGROUND);
        assertEquals("trabajo_abandonar", JobAbandonGUI.BACKGROUND);
        assertEquals("confirmar", ConfirmGUI.BACKGROUND);
        assertEquals("core", CoreMenus.MODULE);
    }

    @Test
    void destructiveConfirmUsesItsOwnBackground() {
        // Abandonar un oficio: botones de color invertido, sin ✓ ni ✗ dibujados encima.
        assertEquals("confirmar", ConfirmGUI.background(false));
        assertEquals("confirmar_peligro", ConfirmGUI.background(true));
    }
}
