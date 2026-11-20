package com.sack.rpgroll.emotes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class EmotesSettingsTest {

    @Test
    void defaultsMatchSpec() {
        EmotesSettings s = EmotesSettings.from(new YamlConfiguration());

        assertEquals(3, s.cooldownSeconds());
        assertEquals(8, s.combatTagSeconds());
        assertEquals(0.3, s.moveDistance());
        assertEquals(0.05, s.walkingSpeed());
        assertEquals(7, s.maxFavorites());
        assertEquals("rpgroll_emotes", s.limbModel());
        assertTrue(s.magicEnabled());
        assertEquals("conjurar", s.castDefault());
        assertEquals("canalizar", s.channelDefault());
        assertTrue(s.castBySchool().isEmpty());
        assertTrue(s.channelBySchool().isEmpty());
        assertTrue(s.sneakSwapHotkey());
        assertTrue(s.quickActionsHotkey());
        assertTrue(s.registerInSrp());
    }

    @Test
    void hotkeysCanBeDisabled() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("hotkeys.sneak-swap", false);
        yaml.set("hotkeys.quick-actions", false);
        EmotesSettings s = EmotesSettings.from(yaml);
        org.junit.jupiter.api.Assertions.assertFalse(s.sneakSwapHotkey());
        org.junit.jupiter.api.Assertions.assertFalse(s.quickActionsHotkey());
    }

    @Test
    void schoolOverridesWin() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("""
                magic:
                  cast:
                    schools:
                      fire: conjurar_fuego
                  channel:
                    schools:
                      frost: canalizar_hielo
                """);
        EmotesSettings s = EmotesSettings.from(yaml);

        assertEquals("conjurar_fuego", s.castEmoteFor("fire"));
        assertEquals("conjurar", s.castEmoteFor("frost"));
        assertEquals("conjurar", s.castEmoteFor(null));
        assertEquals("canalizar_hielo", s.channelEmoteFor("frost"));
        assertEquals("canalizar", s.channelEmoteFor("fire"));
    }

    @Test
    void favoritesMaxIsCappedAt7() {
        java.util.List<String> warnings = new java.util.ArrayList<>();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("favorites.max", 12);
        assertEquals(7, EmotesSettings.from(yaml, warnings::add).maxFavorites());
        assertEquals(1, warnings.size());
        yaml.set("favorites.max", 0);
        assertEquals(1, EmotesSettings.from(yaml).maxFavorites());
        yaml.set("favorites.max", 5);
        assertEquals(5, EmotesSettings.from(yaml, warnings::add).maxFavorites());
        assertEquals(1, warnings.size());
    }
}
