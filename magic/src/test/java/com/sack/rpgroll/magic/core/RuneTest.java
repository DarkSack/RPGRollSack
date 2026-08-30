package com.sack.rpgroll.magic.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuneTest {

    @Test
    void constructorRejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new Rune(null, "Rune", null, null, RuneModifierType.PIERCING, null));
    }

    @Test
    void constructorRejectsNullType() {
        assertThrows(NullPointerException.class,
                () -> new Rune("rune-1", "Rune", null, null, null, null));
    }

    @Test
    void blankDisplayNameFallsBackToId() {
        Rune rune = new Rune("rune-1", " ", null, null, RuneModifierType.PIERCING, null);
        assertEquals("rune-1", rune.displayName());
    }

    @Test
    void blankIconDefaultsToEmerald() {
        Rune rune = new Rune("rune-1", "Rune", "", null, RuneModifierType.PIERCING, null);
        assertEquals("EMERALD", rune.icon());
    }

    @Test
    void nullDescriptionDefaultsToEmpty() {
        Rune rune = new Rune("rune-1", "Rune", null, null, RuneModifierType.PIERCING, null);
        assertEquals("", rune.description());
    }

    @Test
    void nullParamsBecomesEmptyMap() {
        Rune rune = new Rune("rune-1", "Rune", null, null, RuneModifierType.PIERCING, null);
        assertEquals(Map.of(), rune.params());
    }

    @Test
    void paramReturnsFallbackWhenKeyMissing() {
        Rune rune = new Rune("rune-1", "Rune", null, null, RuneModifierType.PIERCING, Map.of());
        assertEquals("default", rune.param("missing", "default"));
    }

    @Test
    void paramReturnsStoredValue() {
        Rune rune = new Rune("rune-1", "Rune", null, null, RuneModifierType.PIERCING, Map.of("key", "value"));
        assertEquals("value", rune.param("key", "default"));
    }

    @Test
    void paramDoubleParsesValidNumber() {
        Rune rune = new Rune("rune-1", "Rune", null, null, RuneModifierType.EXPLOSIVE, Map.of("radius", "3.5"));
        assertEquals(3.5, rune.paramDouble("radius", 1.0));
    }

    @Test
    void paramDoubleFallsBackOnMissingKey() {
        Rune rune = new Rune("rune-1", "Rune", null, null, RuneModifierType.EXPLOSIVE, Map.of());
        assertEquals(1.0, rune.paramDouble("radius", 1.0));
    }

    @Test
    void paramDoubleFallsBackOnMalformedNumber() {
        Rune rune = new Rune("rune-1", "Rune", null, null, RuneModifierType.EXPLOSIVE, Map.of("radius", "abc"));
        assertEquals(1.0, rune.paramDouble("radius", 1.0));
    }

    @Test
    void paramIntParsesValidNumber() {
        Rune rune = new Rune("rune-1", "Rune", null, null, RuneModifierType.EXTRA_PROJECTILES, Map.of("count", "3"));
        assertEquals(3, rune.paramInt("count", 1));
    }

    @Test
    void paramIntFallsBackOnMalformedNumber() {
        Rune rune = new Rune("rune-1", "Rune", null, null, RuneModifierType.EXTRA_PROJECTILES, Map.of("count", "xyz"));
        assertEquals(1, rune.paramInt("count", 1));
    }
}
