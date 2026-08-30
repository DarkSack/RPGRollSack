package com.sack.rpgroll.traps.core;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrapDefinitionTest {

    @Test
    void nullIdThrows() {
        assertThrows(NullPointerException.class, () -> new TrapDefinition(null, "n", "", null, TrapTrigger.PRESSURE,
                null, 1, null, null, 0, -1, null, null, null));
    }

    @Test
    void blankIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TrapDefinition("  ", "n", "", null,
                TrapTrigger.PRESSURE, null, 1, null, null, 0, -1, null, null, null));
    }

    @Test
    void nullTriggerThrows() {
        assertThrows(NullPointerException.class, () -> new TrapDefinition("id", "n", "", null, null, null, 1, null,
                null, 0, -1, null, null, null));
    }

    @Test
    void blankDisplayNameFallsBackToId() {
        TrapDefinition def = new TrapDefinition("landmine", "  ", "", null, TrapTrigger.PRESSURE, null, 1, null,
                null, 0, -1, null, null, null);

        assertEquals("landmine", def.displayName());
    }

    @Test
    void nullCollectionsBecomeEmptyImmutableDefaults() {
        TrapDefinition def = new TrapDefinition("id", "n", null, null, TrapTrigger.PRESSURE, null, 1, null, null, 0,
                -1, null, null, null);

        assertEquals("", def.description());
        assertEquals(Map.of(), def.triggerParams());
        assertEquals(List.of(), def.conditions());
        assertEquals(List.of(), def.actions());
        assertEquals(List.of(), def.chain());
        assertEquals(Material.TRIPWIRE_HOOK, def.icon());
        assertEquals(TrapBlockConfig.none(), def.block());
        assertEquals(TrapDisguiseConfig.none(), def.disguise());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, -100})
    void nonPositiveRadiusFallsBackToDefault(double radius) {
        TrapDefinition def = new TrapDefinition("id", "n", "", null, TrapTrigger.PRESSURE, null, radius, null, null,
                0, -1, null, null, null);

        assertEquals(1.5, def.radius());
    }

    @Test
    void zeroChargesIsNormalizedToInfinite() {
        TrapDefinition def = new TrapDefinition("id", "n", "", null, TrapTrigger.PRESSURE, null, 1, null, null, 0, 0,
                null, null, null);

        assertEquals(-1, def.charges());
        assertTrue(def.hasInfiniteCharges());
    }

    @Test
    void positiveChargesAreKeptAsIsAndAreFinite() {
        TrapDefinition def = new TrapDefinition("id", "n", "", null, TrapTrigger.PRESSURE, null, 1, null, null, 0, 5,
                null, null, null);

        assertEquals(5, def.charges());
        assertFalse(def.hasInfiniteCharges());
    }

    @Test
    void triggerParamFallsBackWhenKeyMissing() {
        TrapDefinition def = new TrapDefinition("id", "n", "", null, TrapTrigger.TIMER,
                Map.of("interval-seconds", "10"), 1, null, null, 0, -1, null, null, null);

        assertEquals("10", def.triggerParam("interval-seconds", "999"));
        assertEquals("999", def.triggerParam("missing", "999"));
    }
}
