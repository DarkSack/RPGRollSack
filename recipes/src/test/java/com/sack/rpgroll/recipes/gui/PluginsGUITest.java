package com.sack.rpgroll.recipes.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginsGUITest {

    @Test
    void sinTraduccionElApartadoSeLlamaComoElPluginSinElPrefijo() {
        assertEquals("Items", PluginsGUI.shortName("RPGRoll-Items"));
        assertEquals("machines", PluginsGUI.shortName("rpgroll-machines"));
        assertEquals("SackitoNetwork", PluginsGUI.shortName("SackitoNetwork"));
        assertEquals("RPGRoll", PluginsGUI.shortName("RPGRoll"));
    }
}
