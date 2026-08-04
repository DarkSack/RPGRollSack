package com.sack.rpgroll.effects.api;

import com.sack.rpgroll.effects.condition.EffectConditionEvaluator;
import com.sack.rpgroll.effects.core.EffectComponentType;
import com.sack.rpgroll.effects.core.EffectDefinition;
import com.sack.rpgroll.effects.core.EffectManager;
import com.sack.rpgroll.effects.core.EffectTriggerType;
import com.sack.rpgroll.effects.engine.EffectComponentExecutor;
import com.sack.rpgroll.effects.runtime.ActiveEffect;
import com.sack.rpgroll.effects.runtime.ApplyResult;
import com.sack.rpgroll.effects.runtime.EffectTracker;
import com.sack.rpgroll.effects.runtime.RemovalReason;

import org.bukkit.entity.LivingEntity;

import java.util.Collection;
import java.util.List;

/**
 * Punto de entrada público de RPGRoll-Effects para cualquier otro addon:
 * items, encantamientos, mobs, dungeons, quests, o cualquier plugin externo.
 * <ul>
 * <li>{@link #apply(String, LivingEntity)} y variantes — aplica un efecto ya
 * definido (YAML o registrado por código) por su id, chequeando condiciones,
 * inmunidad, conflictos y stacking automáticamente.</li>
 * <li>{@link #remove(String, LivingEntity)}/{@link #removeAll(LivingEntity)}
 * — quita efectos activos.</li>
 * <li>{@link #isSilenced(LivingEntity)}/{@link #isConfused(LivingEntity)} —
 * para que otros addons (ej. un futuro sistema de hechizos) consulten si
 * deben bloquear una acción.</li>
 * </ul>
 */
public final class EffectsAPI {

    private static EffectsAPI instance;

    private final EffectManager effectManager;
    private final EffectTracker tracker;
    private final EffectComponentExecutor executor;
    private final EffectConditionEvaluator conditionEvaluator;

    private EffectsAPI(EffectManager effectManager, EffectTracker tracker, EffectComponentExecutor executor,
            EffectConditionEvaluator conditionEvaluator) {
        this.effectManager = effectManager;
        this.tracker = tracker;
        this.executor = executor;
        this.conditionEvaluator = conditionEvaluator;
    }

    public static void init(EffectManager effectManager, EffectTracker tracker, EffectComponentExecutor executor,
            EffectConditionEvaluator conditionEvaluator) {
        instance = new EffectsAPI(effectManager, tracker, executor, conditionEvaluator);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /** @throws IllegalStateException si RPGRoll-Effects todavía no terminó de arrancar. */
    public static EffectsAPI get() {

        if (instance == null) {
            throw new IllegalStateException("RPGRoll-Effects todavía no está listo.");
        }

        return instance;
    }

    // ============ Aplicar / remover ============

    public boolean apply(String effectId, LivingEntity target) {
        return apply(effectId, target, null);
    }

    public boolean apply(String effectId, LivingEntity target, LivingEntity source) {

        var definitionOpt = effectManager.get(effectId);

        if (definitionOpt.isEmpty()) {
            return false;
        }

        if (!conditionEvaluator.check(target, definitionOpt.get()).isEmpty()) {
            return false;
        }

        return applyInternal(definitionOpt.get(), target, source);
    }

    /** Motivos de rechazo si se intentara aplicar ahora mismo (vacío = se puede aplicar). Útil para mensajes de UI. */
    public List<String> checkConditions(String effectId, LivingEntity target) {

        var definitionOpt = effectManager.get(effectId);

        if (definitionOpt.isEmpty()) {
            return List.of("No existe un efecto con id: " + effectId);
        }

        return conditionEvaluator.check(target, definitionOpt.get());
    }

    private boolean applyInternal(EffectDefinition definition, LivingEntity target, LivingEntity source) {

        ApplyResult result = tracker.apply(target, definition, source);

        if (result == ApplyResult.UPGRADE_READY) {

            tracker.remove(target, definition.id(), RemovalReason.UPGRADED);
            var upgraded = effectManager.get(definition.upgradeToEffectId());

            return upgraded.isPresent() && applyInternal(upgraded.get(), target, source);
        }

        return result != ApplyResult.BLOCKED_IMMUNE;
    }

    public boolean remove(String effectId, LivingEntity target) {
        return tracker.remove(target, effectId, RemovalReason.MANUAL);
    }

    public void removeAll(LivingEntity target) {
        tracker.removeAll(target);
    }

    public boolean hasEffect(String effectId, LivingEntity target) {
        return tracker.hasEffect(target, effectId);
    }

    public Collection<ActiveEffect> getActive(LivingEntity target) {
        return tracker.getActive(target);
    }

    // ============ Inmunidades ============

    public void addImmunity(LivingEntity target, String tag) {
        tracker.addImmunity(target, tag);
    }

    public void removeImmunity(LivingEntity target, String tag) {
        tracker.removeImmunity(target, tag);
    }

    public boolean isImmune(LivingEntity target, String tag) {
        return tracker.isImmune(target, tag);
    }

    // ============ Banderas de estado (para que otros addons las consulten) ============

    public boolean isSilenced(LivingEntity target) {
        return hasComponentType(target, EffectComponentType.SILENCE);
    }

    public boolean isConfused(LivingEntity target) {
        return hasComponentType(target, EffectComponentType.CONFUSION);
    }

    private boolean hasComponentType(LivingEntity target, EffectComponentType type) {
        return tracker.getActive(target).stream()
                .flatMap(active -> active.definition().components().stream())
                .anyMatch(component -> component.type() == type);
    }

    // ============ Triggers manuales (extensión para otros addons: región/combate/hechizo) ============

    /**
     * Dispara manualmente un trigger sobre los efectos activos de {@code target}
     * — pensado para que otros addons conecten sus propios eventos (ej. Quests
     * podría llamar esto con REGION_ENTER/REGION_EXIT) sin que este módulo
     * tenga que depender de ellos.
     */
    public void fireTrigger(EffectTriggerType trigger, LivingEntity target) {
        for (ActiveEffect activeEffect : tracker.getActive(target)) {
            executor.fireTrigger(target, activeEffect, trigger);
        }
    }

    // ============ Acceso interno (GUI/comandos del propio addon) ============

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public EffectTracker getTracker() {
        return tracker;
    }

}
