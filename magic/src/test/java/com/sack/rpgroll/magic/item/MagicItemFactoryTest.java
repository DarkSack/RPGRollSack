package com.sack.rpgroll.magic.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicItemFactoryTest {

    @Test
    void laDescripcionSeParteEnLineasCortasSinPerderPalabras() {

        String text = "Un tomo introductorio que enseña los hechizos básicos de Fuego y Escarcha, más sus evoluciones.";
        List<Component> lines = MagicItemFactory.description(text);
        StringBuilder joined = new StringBuilder();

        for (Component line : lines) {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            assertTrue(plain.length() <= 40, plain);
            joined.append(joined.length() > 0 ? " " : "").append(plain);
        }

        assertTrue(lines.size() > 1);
        assertEquals(text, joined.toString());
    }

}
