package com.sack.rpgroll.tab.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Lo que cada jugador eligió mostrar u ocultar en su cabecera y pie. */
class BlockChoicesTest {

    @Test
    void withoutAChoiceTheBlockDefaultWins() {
        BlockChoices choices = BlockChoices.parse(null);
        assertTrue(choices.isShown("personaje", true));
        assertFalse(choices.isShown("servidor", false));
    }

    @Test
    void aChoiceOverridesTheDefault() {
        BlockChoices choices = BlockChoices.parse("personaje=0,servidor=1");
        assertFalse(choices.isShown("personaje", true));
        assertTrue(choices.isShown("servidor", false));
    }

    @Test
    void toggleFlipsWhatIsShownAndSurvivesARoundTrip() {
        BlockChoices choices = BlockChoices.parse("").toggle("progreso", true);
        assertFalse(choices.isShown("progreso", true));
        BlockChoices again = BlockChoices.parse(choices.serialize());
        assertFalse(again.isShown("progreso", true));
        assertTrue(again.toggle("progreso", true).isShown("progreso", true));
    }

    @Test
    void garbageIsIgnored() {
        BlockChoices choices = BlockChoices.parse("roto,=1,a=,b=x,c=1");
        assertTrue(choices.isShown("c", false));
        assertEquals("c=1", choices.serialize());
    }
}
