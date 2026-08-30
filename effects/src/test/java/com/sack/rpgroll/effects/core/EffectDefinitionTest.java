package com.sack.rpgroll.effects.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectDefinitionTest {

    private EffectDefinition build(String id, int durationTicks, int maxStacks, EffectStackingMode mode) {
        return new EffectDefinition(id, "Name", null, null, null, null, null, durationTicks, 0, true, null, null,
                null, mode, maxStacks, null, null);
    }

    @Test
    void constructorRejectsNullId() {
        assertThrows(NullPointerException.class, () -> build(null, 0, 1, EffectStackingMode.NONE));
    }

    @Test
    void blankDisplayNameFallsBackToId() {
        EffectDefinition effect = new EffectDefinition("burn", " ", null, null, null, null, null, 0, 0, true, null,
                null, null, EffectStackingMode.NONE, 1, null, null);
        assertEquals("burn", effect.displayName());
    }

    @Test
    void blankIconDefaultsToNetherStar() {
        EffectDefinition effect = new EffectDefinition("burn", "Burn", "", null, null, null, null, 0, 0, true, null,
                null, null, EffectStackingMode.NONE, 1, null, null);
        assertEquals("NETHER_STAR", effect.icon());
    }

    @Test
    void blankColorDefaultsToWhite() {
        EffectDefinition effect = new EffectDefinition("burn", "Burn", null, "", null, null, null, 0, 0, true, null,
                null, null, EffectStackingMode.NONE, 1, null, null);
        assertEquals("WHITE", effect.color());
    }

    @Test
    void nullCategoryDefaultsToOther() {
        EffectDefinition effect = build("burn", 0, 1, EffectStackingMode.NONE);
        assertEquals(EffectCategory.OTHER, effect.category());
    }

    @Test
    void nullRarityDefaultsToCommon() {
        EffectDefinition effect = build("burn", 0, 1, EffectStackingMode.NONE);
        assertEquals(EffectRarity.COMMON, effect.rarity());
    }

    @Test
    void nullStackingModeDefaultsToNone() {
        EffectDefinition effect = new EffectDefinition("burn", "Burn", null, null, null, null, null, 0, 0, true,
                null, null, null, null, 1, null, null);
        assertEquals(EffectStackingMode.NONE, effect.stackingMode());
    }

    @Test
    void negativeDurationTicksClampsToZero() {
        EffectDefinition effect = build("burn", -50, 1, EffectStackingMode.NONE);
        assertEquals(0, effect.durationTicks());
    }

    @Test
    void maxStacksBelowOneClampsToOne() {
        EffectDefinition effect = build("burn", 0, 0, EffectStackingMode.NONE);
        assertEquals(1, effect.maxStacks());

        EffectDefinition negative = build("burn", 0, -3, EffectStackingMode.NONE);
        assertEquals(1, negative.maxStacks());
    }

    @Test
    void nullCollectionsBecomeEmpty() {
        EffectDefinition effect = build("burn", 0, 1, EffectStackingMode.NONE);

        assertTrue(effect.conditions().isEmpty());
        assertTrue(effect.tags().isEmpty());
        assertTrue(effect.conflicts().isEmpty());
        assertTrue(effect.components().isEmpty());
    }
}
