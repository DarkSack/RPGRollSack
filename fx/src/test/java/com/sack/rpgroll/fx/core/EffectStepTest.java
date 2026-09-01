package com.sack.rpgroll.fx.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EffectStepTest {

    @Test
    void constructorRejectsNullType() {
        assertThrows(NullPointerException.class, () -> new EffectStep(null, 0, null));
    }

    @Test
    void negativeDelayTicksClampsToZero() {
        EffectStep step = new EffectStep(EffectStepType.PARTICLE, -20, null);
        assertEquals(0, step.delayTicks());
    }

    @Test
    void nullParamsBecomesEmptyMap() {
        EffectStep step = new EffectStep(EffectStepType.PARTICLE, 0, null);
        assertEquals(Map.of(), step.params());
    }

    @Test
    void paramReturnsFallbackWhenMissing() {
        EffectStep step = new EffectStep(EffectStepType.PARTICLE, 0, Map.of());
        assertEquals("POINT", step.param("shape", "POINT"));
    }

    @Test
    void paramDoubleParsesValidValue() {
        EffectStep step = new EffectStep(EffectStepType.PARTICLE, 0, Map.of("radius", "2.5"));
        assertEquals(2.5, step.paramDouble("radius", 1.0));
    }

    @Test
    void paramDoubleFallsBackOnMalformedValue() {
        EffectStep step = new EffectStep(EffectStepType.PARTICLE, 0, Map.of("radius", "notanumber"));
        assertEquals(1.0, step.paramDouble("radius", 1.0));
    }

    @Test
    void paramIntParsesValidValue() {
        EffectStep step = new EffectStep(EffectStepType.PARTICLE, 0, Map.of("points", "40"));
        assertEquals(40, step.paramInt("points", 20));
    }

    @Test
    void paramIntFallsBackOnMalformedValue() {
        EffectStep step = new EffectStep(EffectStepType.PARTICLE, 0, Map.of("points", "abc"));
        assertEquals(20, step.paramInt("points", 20));
    }

    @Test
    void paramTargetParsesValidEnumValueCaseInsensitively() {
        EffectStep step = new EffectStep(EffectStepType.PARTICLE, 0, Map.of("target", "target"));
        assertEquals(EffectTarget.TARGET, step.paramTarget("target", EffectTarget.SELF));
    }

    @Test
    void paramTargetFallsBackOnInvalidEnumValue() {
        EffectStep step = new EffectStep(EffectStepType.PARTICLE, 0, Map.of("target", "NOT_A_TARGET"));
        assertEquals(EffectTarget.SELF, step.paramTarget("target", EffectTarget.SELF));
    }

    @Test
    void paramTargetFallsBackWhenKeyMissing() {
        EffectStep step = new EffectStep(EffectStepType.PARTICLE, 0, Map.of());
        assertEquals(EffectTarget.SELF, step.paramTarget("target", EffectTarget.SELF));
    }
}
