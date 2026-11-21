package com.sack.rpgroll.common.command;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandWordsTest {

    private static CommandWords words() throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(String.join("\n",
                "commands:",
                "  guild:",
                "    names: {es: gremio, pt: guilda}",
                "    subcommands:",
                "      create: {es: crear, pt: criar}",
                "      info: {es: info}",
                "  tienda:",
                "    names: {en: shop, pt: loja}"));
        return CommandWords.parse(yaml);
    }

    @Test
    void everyLanguageNameBecomesAnAlias() throws InvalidConfigurationException {
        assertEquals(List.of("gremio", "guilda"), words().names("guild"));
        assertEquals(List.of("shop", "loja"), words().names("tienda"));
        assertEquals(List.of(), words().names("quest"));
    }

    @Test
    void aSubcommandInAnyLanguageMeansTheSame() throws InvalidConfigurationException {
        CommandWords words = words();
        assertEquals("create", words.canonical("guild", "crear"));
        assertEquals("create", words.canonical("guild", "CRIAR"));
        assertEquals("create", words.canonical("guild", "create"));
        assertEquals("kick", words.canonical("guild", "kick"), "lo que no está en la tabla pasa tal cual");
        assertEquals("crear", words.canonical("quest", "crear"));
    }

    @Test
    void suggestionsAreShownInThePlayersLanguage() throws InvalidConfigurationException {
        CommandWords words = words();
        assertEquals("crear", words.localized("guild", "create", "es"));
        assertEquals("criar", words.localized("guild", "create", "pt"));
        assertEquals("create", words.localized("guild", "create", "en"));
        assertEquals("kick", words.localized("guild", "kick", "es"));
    }

    @Test
    void suggestFiltersByWhatWasTypedInEitherLanguage() throws InvalidConfigurationException {
        CommandWords words = words();
        List<String> all = List.of("create", "info", "kick");
        assertEquals(List.of("crear"), words.suggestFirst("guild", all, "cr", "es"));
        assertEquals(List.of("crear", "info", "kick"), words.suggestFirst("guild", all, "", "es"));
        // Quien escribe en inglés sigue viendo lo suyo.
        assertEquals(List.of("create"), words.suggestFirst("guild", all, "creat", "es"));
        assertTrue(words.suggestFirst("guild", all, "zz", "es").isEmpty());
    }

    @Test
    void languageCodesFallBackToEnglish() {
        assertEquals("es", CommandWords.language(java.util.Locale.forLanguageTag("es-MX")));
        assertEquals("pt", CommandWords.language(java.util.Locale.forLanguageTag("pt-BR")));
        assertEquals("en", CommandWords.language(java.util.Locale.GERMANY));
    }
}
