package com.sack.rpgroll.structures.config;

import com.sack.rpgroll.structures.config.StructuresSettings.BreakPolicy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StructuresSettingsTest {

    private static YamlConfiguration yaml(String s) {
        try {
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(s);
            return y;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void breakPolicyDefaultsToBlockAndRejectsUnknown() {
        List<String> warns = new ArrayList<>();
        assertEquals(BreakPolicy.BLOCK, StructuresSettings.from(yaml(""), warns::add).breakPolicy());
        assertTrue(warns.isEmpty());
        assertEquals(BreakPolicy.BLOCK, StructuresSettings.from(yaml("break-policy: nope"), warns::add).breakPolicy());
        assertEquals(1, warns.size());
        assertEquals(BreakPolicy.PAUSE, StructuresSettings.from(yaml("break-policy: Pause"), warns::add).breakPolicy());
    }

    @Test
    void readsLanguageAndMenusPack() {
        var s = StructuresSettings.from(yaml("language: en\nmenus:\n  pack: 'sackito:menus'"), w -> fail(w));
        assertEquals("en", s.language());
        assertEquals("sackito:menus", s.menusPack());
        assertEquals("es", StructuresSettings.from(yaml(""), w -> fail(w)).language());
    }
}
