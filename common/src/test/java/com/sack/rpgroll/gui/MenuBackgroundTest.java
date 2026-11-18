package com.sack.rpgroll.gui;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuBackgroundTest {

    @Test
    void blankBackgroundKeepsTitle() {
        Component t = Component.text("Cantera");
        assertSame(t, MenuBackground.title("", t));
        assertSame(t, MenuBackground.title(null, t));
        assertFalse(MenuBackground.active(" "));
    }

    @Test
    void invalidKeyKeepsTitle() {
        Component t = Component.text("Cantera");
        // Mayúsculas: no es una clave válida del paquete de recursos.
        assertSame(t, MenuBackground.title("sackito:Menus/Cantera", t));
        assertFalse(MenuBackground.active("sin dos puntos y espacios"));
        assertTrue(MenuBackground.active("sackito:menus/cantera"));
    }

    @Test
    void glyphGoesBeforeTitleWithItsFont() {
        Component c = MenuBackground.title("sackito:menus/cantera", Component.text("Cantera"));
        assertEquals("Cantera", PlainTextComponentSerializer.plainText().serialize(c));
        Component glyph = c.children().getFirst();
        assertEquals(Key.key("sackito:menus/cantera"), glyph.font());
        assertEquals(NamedTextColor.WHITE, glyph.color());
        // El título sigue con la fuente normal.
        assertNull(c.children().get(1).font());
    }
}
