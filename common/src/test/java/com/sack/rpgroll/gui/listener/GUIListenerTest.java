package com.sack.rpgroll.gui.listener;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GUIListenerTest {

    @Test
    void dragThatReachesTheMenuIsBlocked() {
        // Menú de 36 casillas: 0-35 son del menú, 36 en adelante del inventario del jugador.
        assertTrue(GUIListener.touchesMenu(Set.of(12, 40), 36));
        assertTrue(GUIListener.touchesMenu(Set.of(35), 36));
        assertFalse(GUIListener.touchesMenu(Set.of(36, 50, 62), 36));
    }
}
