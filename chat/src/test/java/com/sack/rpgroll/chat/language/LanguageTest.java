package com.sack.rpgroll.chat.language;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageTest {

    @Test
    void obfuscatePreservesWhitespaceButMasksLetters() {
        Language language = new Language("common", "Common", '?', List.of());

        assertEquals("???? ????", language.obfuscate("some text"));
    }

    @Test
    void obfuscateOfEmptyStringIsEmpty() {
        Language language = new Language("common", "Common", '?', List.of());
        assertEquals("", language.obfuscate(""));
    }

    @Test
    void blankDisplayNameFallsBackToId() {
        Language language = new Language("common", "  ", '?', null);
        assertEquals("common", language.displayName());
    }

    @Test
    void nullDefaultForRacesBecomesEmptyList() {
        Language language = new Language("common", "Common", '?', null);
        assertEquals(0, language.defaultForRaces().size());
    }
}
