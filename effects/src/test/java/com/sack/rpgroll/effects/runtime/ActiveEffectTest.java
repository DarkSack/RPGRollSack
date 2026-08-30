package com.sack.rpgroll.effects.runtime;

import com.sack.rpgroll.effects.core.EffectDefinition;
import com.sack.rpgroll.effects.core.EffectStackingMode;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveEffectTest {

    private EffectDefinition definition(int durationTicks, int maxStacks) {
        return new EffectDefinition("burn", "Burn", null, null, null, null, null, durationTicks, 0, true, null,
                null, null, EffectStackingMode.REFRESH, maxStacks, null, null);
    }

    @Test
    void newActiveEffectStartsWithOneStackAndFullDuration() {
        ActiveEffect effect = new ActiveEffect(definition(100, 3), null);

        assertEquals(1, effect.stacks());
        assertEquals(100, effect.remainingTicks());
        assertEquals(0, effect.ticksAlive());
    }

    @Test
    void isPermanentTrueWhenDurationTicksIsZero() {
        assertTrue(new ActiveEffect(definition(0, 1), null).isPermanent());
    }

    @Test
    void isPermanentFalseWhenDurationTicksIsPositive() {
        assertFalse(new ActiveEffect(definition(20, 1), null).isPermanent());
    }

    @Test
    void resetDurationRestoresFullRemainingTicks() {
        ActiveEffect effect = new ActiveEffect(definition(100, 1), null);

        effect.decrementTick();
        effect.decrementTick();
        effect.resetDuration();

        assertEquals(100, effect.remainingTicks());
    }

    @Test
    void incrementStacksCapsAtMaxStacks() {
        ActiveEffect effect = new ActiveEffect(definition(100, 3), null);

        assertEquals(2, effect.incrementStacks(3));
        assertEquals(3, effect.incrementStacks(3));
        assertEquals(3, effect.incrementStacks(3));
    }

    @Test
    void decrementTickReturnsFalseWhileTicksRemain() {
        ActiveEffect effect = new ActiveEffect(definition(2, 1), null);

        assertFalse(effect.decrementTick());
        assertEquals(1, effect.remainingTicks());
    }

    @Test
    void decrementTickReturnsTrueWhenReachingZero() {
        ActiveEffect effect = new ActiveEffect(definition(1, 1), null);

        assertTrue(effect.decrementTick());
        assertEquals(0, effect.remainingTicks());
    }

    @Test
    void decrementTickNeverExpiresPermanentEffect() {
        ActiveEffect effect = new ActiveEffect(definition(0, 1), null);

        for (int i = 0; i < 5; i++) {
            assertFalse(effect.decrementTick());
        }
        assertEquals(5, effect.ticksAlive());
    }

    @Test
    void ticksAliveIncrementsEvenForPermanentEffects() {
        ActiveEffect effect = new ActiveEffect(definition(0, 1), null);

        effect.decrementTick();
        effect.decrementTick();
        effect.decrementTick();

        assertEquals(3, effect.ticksAlive());
    }

    @Test
    void sourceIdIsPreservedWhenProvided() {
        UUID source = UUID.randomUUID();
        ActiveEffect effect = new ActiveEffect(definition(100, 1), source);

        assertEquals(source, effect.sourceId());
    }

    @Test
    void sourceIdIsNullWhenNoCauser() {
        ActiveEffect effect = new ActiveEffect(definition(100, 1), null);

        assertEquals(null, effect.sourceId());
    }
}
