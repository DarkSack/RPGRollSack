package com.sack.rpgroll.recipes.gui;

import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CatalogGUITest {

    private static final List<String> STATIONS = List.of("crafting_table", "furnace", "smoker");

    @Test
    void izquierdoAvanzaYVuelveATodas() {
        assertEquals("crafting_table", CatalogGUI.cycle(STATIONS, null, ClickType.LEFT));
        assertEquals("furnace", CatalogGUI.cycle(STATIONS, "crafting_table", ClickType.LEFT));
        assertNull(CatalogGUI.cycle(STATIONS, "smoker", ClickType.LEFT));
    }

    @Test
    void derechoRetrocedeDandoLaVuelta() {
        assertEquals("smoker", CatalogGUI.cycle(STATIONS, null, ClickType.RIGHT));
        assertNull(CatalogGUI.cycle(STATIONS, "crafting_table", ClickType.RIGHT));
        assertEquals("crafting_table", CatalogGUI.cycle(STATIONS, "furnace", ClickType.RIGHT));
    }

    @Test
    void mayusculasQuitaElFiltro() {
        assertNull(CatalogGUI.cycle(STATIONS, "furnace", ClickType.SHIFT_LEFT));
        assertNull(CatalogGUI.cycle(List.of(), null, ClickType.LEFT));
    }
}
