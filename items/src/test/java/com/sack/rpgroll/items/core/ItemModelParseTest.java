package com.sack.rpgroll.items.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemModelParseTest {

    @Test
    void itemModelIsNamespacedAndLowercase() {
        assertEquals("sackito:lingote_mitrilo", ItemParser.parseItemModel(" Sackito:Lingote_Mitrilo ", "x"));
        assertEquals("minecraft:diamond", ItemParser.parseItemModel("diamond", "x"));
        assertEquals("sackito:armas/espada_1", ItemParser.parseItemModel("sackito:armas/espada_1", "x"));
        assertNull(ItemParser.parseItemModel("  ", "x"));
        assertNull(ItemParser.parseItemModel(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> ItemParser.parseItemModel("sackito:con espacio", "x"));
        assertThrows(IllegalArgumentException.class, () -> ItemParser.parseItemModel("a:b:c", "x"));
    }

}
