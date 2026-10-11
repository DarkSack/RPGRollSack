package com.sack.rpgroll.common.content;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentLoaderTest {

    private static YamlConfiguration yaml() throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(String.join("\n",
                "display-name: Orco",
                "lore: [uno, dos]",
                "talents:",
                "  inicio:",
                "    display-name: Inicio",
                "    cost: 1",
                "translations:",
                "  en:",
                "    display-name: Orc",
                "    lore: [one]",
                "    talents:",
                "      inicio:",
                "        display-name: Start"));
        return config;
    }

    /** El idioma del servidor pisa solo los campos que traduce, también los anidados. */
    @Test
    void translationOverridesOnlyItsOwnFields() throws InvalidConfigurationException {
        YamlConfiguration config = ContentLoader.translate(yaml(), "en");

        assertEquals("Orc", config.getString("display-name"));
        assertEquals(List.of("one"), config.getStringList("lore"));
        assertEquals("Start", config.getString("talents.inicio.display-name"));
        assertEquals(1, config.getInt("talents.inicio.cost"));
    }

    @Test
    void aLocaleWithoutTranslationLeavesTheFileAsIs() throws InvalidConfigurationException {
        YamlConfiguration config = ContentLoader.translate(yaml(), "pt_BR");

        assertEquals("Orco", config.getString("display-name"));
        assertEquals(List.of("uno", "dos"), config.getStringList("lore"));
    }

}
