package com.sack.rpgroll.traps.gui;

import com.sack.rpgroll.traps.gui.turret.CameraMonitorGUI;
import com.sack.rpgroll.traps.gui.turret.TurretAmmoGUI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los ids de fondo tienen que ser los de infra/maquinas/menus_jugador.py (sackito-network). */
class MenuBackgroundIdsTest {

    @Test
    void idsMatchPack() {
        assertEquals("camaras", CameraMonitorGUI.BACKGROUND);
        assertEquals("torreta_municion", TurretAmmoGUI.BACKGROUND);
        assertEquals("traps", TrapMenus.MODULE);
    }
}
