package com.sack.rpgroll.structures.gui;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class CenterMenuLayoutTest {
    @Test void centerSlotsAreDistinctAndInside() {
        assertEquals(CenterMenu.SLOTS.size(), new HashSet<>(CenterMenu.SLOTS).size());
        CenterMenu.SLOTS.forEach(s -> assertTrue(s >= 0 && s <= 53, "slot " + s));
    }

    @Test void centerSlotsMatchTheBrief() {
        assertEquals(20, CenterMenu.CATALOGO);
        assertEquals(22, CenterMenu.MIS_CONSTRUCCIONES);
        assertEquals(24, CenterMenu.MATERIALES);
        assertEquals(29, CenterMenu.OBRA);
        assertEquals(31, CenterMenu.MEJORAS);
        assertEquals(33, CenterMenu.ACCESO);
        assertEquals(49, CenterMenu.AYUDA);
        assertEquals(53, CenterMenu.CERRAR);
        assertEquals(4, CenterMenu.NIVEL);
    }

    @Test void upgradeRequirementsFitTheirRow() {
        assertTrue(UpgradeMenu.REQUISITOS_DESDE + UpgradeMenu.REQUISITOS_MAX - 1 < 18);
        assertTrue(UpgradeMenu.MEJORAR < 36 && UpgradeMenu.VOLVER < 36);
    }
}
