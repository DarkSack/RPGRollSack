package com.sack.rpgroll.emotes.gui;

import com.sack.rpgroll.common.lang.LangManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RarityNameTest {

    @Test
    void translatedOrIdAsFallback() {
        LangManager lang = mock(LangManager.class);
        // Como LangManager.raw: sin la clave, devuelve la clave misma.
        when(lang.raw(anyString())).thenAnswer(i -> i.getArgument(0));
        when(lang.raw("rarity.uncommon")).thenReturn("Poco común");

        assertEquals("Poco común", EmotesMenu.rarityName(lang, "uncommon"));
        assertEquals("Mitica", EmotesMenu.rarityName(lang, "mitica"));
    }
}
