package com.sack.rpgroll.effects.runtime;

import com.sack.rpgroll.effects.core.EffectDefinition;
import com.sack.rpgroll.effects.core.EffectTriggerType;
import com.sack.rpgroll.effects.engine.EffectComponentExecutor;

import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Estado en memoria de todos los efectos activos, por entidad — aplica,
 * remueve, resuelve stacking/conflictos/inmunidades. No sabe nada de
 * triggers periódicos (eso lo maneja {@code EffectTickTask}) ni de qué hace
 * cada componente en sí (eso lo maneja {@link EffectComponentExecutor}) —
 * solo el "libro contable" de qué está activo en quién.
 */
public class EffectTracker {

    private final Map<UUID, Map<String, ActiveEffect>> active = new HashMap<>();
    private final Map<UUID, Set<String>> immunities = new HashMap<>();
    private final Map<UUID, AtomicInteger> independentStackCounters = new HashMap<>();
    private final EffectComponentExecutor executor;

    public EffectTracker(EffectComponentExecutor executor) {
        this.executor = executor;
    }

    public ApplyResult apply(LivingEntity target, EffectDefinition definition, LivingEntity source) {

        if (isImmuneToAny(target, definition.tags())) {
            return ApplyResult.BLOCKED_IMMUNE;
        }

        Map<String, ActiveEffect> targetEffects = active.computeIfAbsent(target.getUniqueId(),
                k -> new LinkedHashMap<>());

        UUID sourceId = source != null ? source.getUniqueId() : null;

        if (definition.stackingMode() == com.sack.rpgroll.effects.core.EffectStackingMode.INDEPENDENT) {
            resolveConflicts(target, definition, targetEffects);
            String storageKey = definition.id() + "#" + nextIndependentIndex(target);
            ActiveEffect fresh = new ActiveEffect(definition, sourceId);
            targetEffects.put(storageKey, fresh);
            executor.fireTrigger(target, fresh, EffectTriggerType.ON_APPLY);
            return ApplyResult.APPLIED;
        }

        ActiveEffect existing = targetEffects.get(definition.id());

        if (existing == null) {
            resolveConflicts(target, definition, targetEffects);
            ActiveEffect fresh = new ActiveEffect(definition, sourceId);
            targetEffects.put(definition.id(), fresh);
            executor.fireTrigger(target, fresh, EffectTriggerType.ON_APPLY);
            return ApplyResult.APPLIED;
        }

        return switch (definition.stackingMode()) {
            case NONE -> {
                existing.resetDuration();
                yield ApplyResult.REFRESHED;
            }
            case REFRESH -> {
                existing.resetDuration();
                existing.incrementStacks(definition.maxStacks());
                yield ApplyResult.STACKED;
            }
            case UPGRADE -> {
                existing.resetDuration();
                int stacks = existing.incrementStacks(definition.maxStacks());
                yield stacks >= definition.maxStacks() && definition.upgradeToEffectId() != null
                        ? ApplyResult.UPGRADE_READY
                        : ApplyResult.STACKED;
            }
            case INDEPENDENT -> ApplyResult.APPLIED; // inalcanzable, manejado arriba
        };
    }

    /** Remueve TODAS las instancias (incluyendo stacks INDEPENDENT) de un id de efecto. */
    public boolean remove(LivingEntity target, String effectId, RemovalReason reason) {

        Map<String, ActiveEffect> targetEffects = active.get(target.getUniqueId());

        if (targetEffects == null || targetEffects.isEmpty()) {
            return false;
        }

        List<String> keysToRemove = new ArrayList<>();

        for (var entry : targetEffects.entrySet()) {
            if (entry.getValue().definition().id().equals(effectId)) {
                keysToRemove.add(entry.getKey());
            }
        }

        for (String key : keysToRemove) {
            ActiveEffect removed = targetEffects.remove(key);
            EffectTriggerType trigger = reason == RemovalReason.EXPIRED ? EffectTriggerType.ON_EXPIRE
                    : EffectTriggerType.ON_REMOVE;
            executor.fireTrigger(target, removed, trigger);
        }

        return !keysToRemove.isEmpty();
    }

    public void removeAll(LivingEntity target) {

        Map<String, ActiveEffect> targetEffects = active.remove(target.getUniqueId());

        if (targetEffects == null) {
            return;
        }

        for (ActiveEffect effect : targetEffects.values()) {
            executor.fireTrigger(target, effect, EffectTriggerType.ON_REMOVE);
        }
    }

    public boolean hasEffect(LivingEntity target, String effectId) {

        Map<String, ActiveEffect> targetEffects = active.get(target.getUniqueId());

        if (targetEffects == null) {
            return false;
        }

        return targetEffects.values().stream().anyMatch(effect -> effect.definition().id().equals(effectId));
    }

    public Collection<ActiveEffect> getActive(LivingEntity target) {

        Map<String, ActiveEffect> targetEffects = active.get(target.getUniqueId());
        return targetEffects == null ? List.of() : List.copyOf(targetEffects.values());
    }

    public void addImmunity(LivingEntity target, String tag) {
        immunities.computeIfAbsent(target.getUniqueId(), k -> new java.util.HashSet<>()).add(tag.toLowerCase());
    }

    public void removeImmunity(LivingEntity target, String tag) {
        Set<String> tags = immunities.get(target.getUniqueId());
        if (tags != null) {
            tags.remove(tag.toLowerCase());
        }
    }

    public boolean isImmune(LivingEntity target, String tag) {
        Set<String> tags = immunities.get(target.getUniqueId());
        return tags != null && tags.contains(tag.toLowerCase());
    }

    private boolean isImmuneToAny(LivingEntity target, Set<String> tags) {

        if (tags.isEmpty()) {
            return false;
        }

        Set<String> entityImmunities = immunities.get(target.getUniqueId());

        if (entityImmunities == null || entityImmunities.isEmpty()) {
            return false;
        }

        for (String tag : tags) {
            if (entityImmunities.contains(tag.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /** Remueve cualquier efecto activo que esté en conflicto con {@code incoming}, en cualquiera de los dos sentidos. */
    private void resolveConflicts(LivingEntity target, EffectDefinition incoming, Map<String, ActiveEffect> targetEffects) {

        if (targetEffects.isEmpty()) {
            return;
        }

        List<String> toRemove = new ArrayList<>();

        for (var entry : targetEffects.entrySet()) {

            EffectDefinition activeDefinition = entry.getValue().definition();

            boolean conflicts = incoming.conflicts().contains(activeDefinition.id())
                    || activeDefinition.conflicts().contains(incoming.id());

            if (conflicts) {
                toRemove.add(entry.getKey());
            }
        }

        for (String key : toRemove) {
            ActiveEffect removed = targetEffects.remove(key);
            executor.fireTrigger(target, removed, EffectTriggerType.ON_REMOVE);
        }
    }

    private int nextIndependentIndex(LivingEntity target) {
        return independentStackCounters.computeIfAbsent(target.getUniqueId(), k -> new AtomicInteger())
                .incrementAndGet();
    }

    /** Vista cruda para que {@code EffectTickTask} pueda iterar sin copiar en cada tick. */
    Map<UUID, Map<String, ActiveEffect>> rawView() {
        return active;
    }

}
