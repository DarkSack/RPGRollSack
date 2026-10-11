package com.sack.rpgroll.common.content;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

    /** Un editor del panel en un servidor en inglés: cambia el inglés, y el español y el portugués siguen ahí. */
    @Test
    void savingFromAnEditorKeepsEveryTranslation(@org.junit.jupiter.api.io.TempDir File dataFolder)
            throws IOException, InvalidConfigurationException {
        Files.writeString(new File(dataFolder, "config.yml").toPath(), "language: en");
        File target = new File(dataFolder, "races/orco.yml");
        target.getParentFile().mkdirs();
        YamlConfiguration shipped = yaml();
        shipped.set("translations.pt_BR.display-name", "Orc (pt)");
        shipped.save(target);

        YamlConfiguration edited = new YamlConfiguration();
        edited.set("display-name", "Orc Warlord");
        edited.set("lore", List.of("one"));
        ContentLoader.save(edited, target);

        YamlConfiguration saved = YamlConfiguration.loadConfiguration(target);
        assertEquals("Orco", saved.getString("display-name"));
        assertEquals("Orc Warlord", saved.getString("translations.en.display-name"));
        assertEquals("Orc (pt)", saved.getString("translations.pt_BR.display-name"));
        assertEquals("Orc Warlord", ContentLoader.translate(saved, "en").getString("display-name"));
    }

}
