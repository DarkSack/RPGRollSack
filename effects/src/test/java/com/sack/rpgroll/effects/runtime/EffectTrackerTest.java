package com.sack.rpgroll.effects.runtime;

import com.sack.rpgroll.effects.core.EffectDefinition;
import com.sack.rpgroll.effects.core.EffectStackingMode;
import com.sack.rpgroll.effects.core.EffectTriggerType;
import com.sack.rpgroll.effects.engine.EffectComponentExecutor;

import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EffectTrackerTest {

    private EffectComponentExecutor executor;
    private EffectTracker tracker;
    private LivingEntity target;
    private LivingEntity source;

    @BeforeEach
    void setUp() {
        executor = mock(EffectComponentExecutor.class);
        tracker = new EffectTracker(executor);
        target = mock(LivingEntity.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        source = mock(LivingEntity.class);
        when(source.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    private EffectDefinition definition(String id, EffectStackingMode mode, int maxStacks, Set<String> tags,
            Set<String> conflicts) {
        return new EffectDefinition(id, id, null, null, null, null, null, 100, 0, true, null, tags, conflicts, mode,
                maxStacks, null, null);
    }

    @Test
    void applyingNewEffectFiresOnApplyAndReturnsApplied() {
        EffectDefinition definition = definition("burn", EffectStackingMode.NONE, 1, Set.of(), Set.of());

        ApplyResult result = tracker.apply(target, definition, source);

        assertEquals(ApplyResult.APPLIED, result);
        assertTrue(tracker.hasEffect(target, "burn"));
        verify(executor).fireTrigger(eq(target), any(ActiveEffect.class), eq(EffectTriggerType.ON_APPLY));
    }

    @Test
    void reapplyingNoneStackingModeRefreshesInsteadOfStacking() {
        EffectDefinition definition = definition("burn", EffectStackingMode.NONE, 1, Set.of(), Set.of());
        tracker.apply(target, definition, source);

        ApplyResult result = tracker.apply(target, definition, source);

        assertEquals(ApplyResult.REFRESHED, result);
        assertEquals(1, tracker.getActive(target).iterator().next().stacks());
    }

    @Test
    void reapplyingRefreshStackingModeAccumulatesStacksUpToMax() {
        EffectDefinition definition = definition("burn", EffectStackingMode.REFRESH, 2, Set.of(), Set.of());
        tracker.apply(target, definition, source);

        ApplyResult second = tracker.apply(target, definition, source);
        ApplyResult third = tracker.apply(target, definition, source);

        assertEquals(ApplyResult.STACKED, second);
        assertEquals(ApplyResult.STACKED, third);
        assertEquals(2, tracker.getActive(target).iterator().next().stacks());
    }

    @Test
    void reapplyingUpgradeModeReturnsUpgradeReadyOnceMaxStacksReachedWithUpgradeId() {
        EffectDefinition definition = new EffectDefinition("burn", "Burn", null, null, null, null, null, 100, 0,
                true, null, Set.of(), Set.of(), EffectStackingMode.UPGRADE, 2, "burn-2", null);

        tracker.apply(target, definition, source);
        ApplyResult result = tracker.apply(target, definition, source);

        assertEquals(ApplyResult.UPGRADE_READY, result);
    }

    @Test
    void reapplyingUpgradeModeStaysStackedWithoutUpgradeToEffectId() {
        EffectDefinition definition = definition("burn", EffectStackingMode.UPGRADE, 2, Set.of(), Set.of());

        tracker.apply(target, definition, source);
        ApplyResult result = tracker.apply(target, definition, source);

        assertEquals(ApplyResult.STACKED, result);
    }

    @Test
    void independentStackingModeCreatesSeparateInstancesEachTime() {
        EffectDefinition definition = definition("poison", EffectStackingMode.INDEPENDENT, 1, Set.of(), Set.of());

        tracker.apply(target, definition, source);
        tracker.apply(target, definition, source);
        tracker.apply(target, definition, source);

        assertEquals(3, tracker.getActive(target).size());
    }

    @Test
    void applyBlockedWhenTargetImmuneToEffectTag() {
        tracker.addImmunity(target, "fire");
        EffectDefinition definition = definition("burn", EffectStackingMode.NONE, 1, Set.of("fire"), Set.of());

        ApplyResult result = tracker.apply(target, definition, source);

        assertEquals(ApplyResult.BLOCKED_IMMUNE, result);
        assertFalse(tracker.hasEffect(target, "burn"));
    }

    @Test
    void immunityCheckIsCaseInsensitive() {
        tracker.addImmunity(target, "FIRE");
        EffectDefinition definition = definition("burn", EffectStackingMode.NONE, 1, Set.of("fire"), Set.of());

        assertEquals(ApplyResult.BLOCKED_IMMUNE, tracker.apply(target, definition, source));
    }

    @Test
    void removeImmunityRestoresAbilityToApply() {
        tracker.addImmunity(target, "fire");
        tracker.removeImmunity(target, "fire");
        EffectDefinition definition = definition("burn", EffectStackingMode.NONE, 1, Set.of("fire"), Set.of());

        assertEquals(ApplyResult.APPLIED, tracker.apply(target, definition, source));
    }

    @Test
    void applyingConflictingEffectRemovesTheExistingOne() {
        EffectDefinition frost = definition("frost", EffectStackingMode.NONE, 1, Set.of(), Set.of("burn"));
        EffectDefinition burn = definition("burn", EffectStackingMode.NONE, 1, Set.of(), Set.of());

        tracker.apply(target, burn, source);
        tracker.apply(target, frost, source);

        assertFalse(tracker.hasEffect(target, "burn"));
        assertTrue(tracker.hasEffect(target, "frost"));
        verify(executor, times(1)).fireTrigger(eq(target), any(ActiveEffect.class), eq(EffectTriggerType.ON_REMOVE));
    }

    @Test
    void conflictIsDetectedInEitherDirection() {
        EffectDefinition burn = definition("burn", EffectStackingMode.NONE, 1, Set.of(), Set.of("frost"));
        EffectDefinition frost = definition("frost", EffectStackingMode.NONE, 1, Set.of(), Set.of());

        tracker.apply(target, burn, source);
        tracker.apply(target, frost, source);

        assertFalse(tracker.hasEffect(target, "burn"));
        assertTrue(tracker.hasEffect(target, "frost"));
    }

    @Test
    void removeReturnsFalseWhenEffectNotActive() {
        assertFalse(tracker.remove(target, "burn", RemovalReason.MANUAL));
    }

    @Test
    void removeFiresOnExpireForExpiredReason() {
        EffectDefinition definition = definition("burn", EffectStackingMode.NONE, 1, Set.of(), Set.of());
        tracker.apply(target, definition, source);

        boolean removed = tracker.remove(target, "burn", RemovalReason.EXPIRED);

        assertTrue(removed);
        verify(executor).fireTrigger(eq(target), any(ActiveEffect.class), eq(EffectTriggerType.ON_EXPIRE));
    }

    @Test
    void removeFiresOnRemoveForManualReason() {
        EffectDefinition definition = definition("burn", EffectStackingMode.NONE, 1, Set.of(), Set.of());
        tracker.apply(target, definition, source);

        tracker.remove(target, "burn", RemovalReason.MANUAL);

        verify(executor).fireTrigger(eq(target), any(ActiveEffect.class), eq(EffectTriggerType.ON_REMOVE));
    }

    @Test
    void removeOnIndependentStackingRemovesAllInstances() {
        EffectDefinition definition = definition("poison", EffectStackingMode.INDEPENDENT, 1, Set.of(), Set.of());
        tracker.apply(target, definition, source);
        tracker.apply(target, definition, source);

        boolean removed = tracker.remove(target, "poison", RemovalReason.MANUAL);

        assertTrue(removed);
        assertTrue(tracker.getActive(target).isEmpty());
    }

    @Test
    void removeAllClearsEveryEffectOnTarget() {
        tracker.apply(target, definition("burn", EffectStackingMode.NONE, 1, Set.of(), Set.of()), source);
        tracker.apply(target, definition("frost", EffectStackingMode.NONE, 1, Set.of(), Set.of()), source);

        tracker.removeAll(target);

        assertTrue(tracker.getActive(target).isEmpty());
    }

    @Test
    void removeAllOnEntityWithNoEffectsIsANoOp() {
        tracker.removeAll(target);
        assertTrue(tracker.getActive(target).isEmpty());
    }

    @Test
    void hasEffectFalseForEntityWithNoEffectsAtAll() {
        assertFalse(tracker.hasEffect(target, "burn"));
    }

    @Test
    void getActiveReturnsEmptyCollectionForUntrackedEntity() {
        assertTrue(tracker.getActive(target).isEmpty());
    }

    @Test
    void isImmuneReflectsAddedAndRemovedTags() {
        assertFalse(tracker.isImmune(target, "fire"));

        tracker.addImmunity(target, "fire");
        assertTrue(tracker.isImmune(target, "fire"));
        assertTrue(tracker.isImmune(target, "FIRE"));

        tracker.removeImmunity(target, "fire");
        assertFalse(tracker.isImmune(target, "fire"));
    }
}
