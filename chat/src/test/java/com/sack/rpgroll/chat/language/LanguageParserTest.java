package com.sack.rpgroll.chat.language;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageParserTest {

    private final LanguageParser parser = new LanguageParser();

    private YamlConfiguration load(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    @Test
    void missingIdThrows() {
        YamlConfiguration config = load("display-name: Elvish");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void defaultsObfuscationCharToQuestionMark() throws Exception {
        YamlConfiguration config = load("id: common");
        Language language = parser.parse(config);

        assertEquals('?', language.obfuscationChar());
        assertEquals("?? ?????", language.obfuscate("hi there"));
    }

    @Test
    void blankObfuscationCharFallsBackToQuestionMark() throws Exception {
        YamlConfiguration config = load("""
                id: common
                obfuscation-char: ""
                """);

        assertEquals('?', parser.parse(config).obfuscationChar());
    }

    @Test
    void usesOnlyFirstCharacterOfObfuscationString() throws Exception {
        YamlConfiguration config = load("""
                id: elvish
                obfuscation-char: "#*"
                """);

        assertEquals('#', parser.parse(config).obfuscationChar());
    }

    @Test
    void parsesDefaultForRacesList() throws Exception {
        YamlConfiguration config = load("""
                id: elvish
                default-for-races:
                  - elf
                  - half-elf
                """);

        Language language = parser.parse(config);
        assertEquals(2, language.defaultForRaces().size());
        assertTrue(language.defaultForRaces().contains("elf"));
    }
}
