package com.sack.rpgroll.effects.runtime;

import com.sack.rpgroll.effects.core.EffectTriggerType;
import com.sack.rpgroll.effects.engine.EffectComponentExecutor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Corre cada tick del servidor: decrementa la duración de cada efecto
 * activo, dispara ON_TICK/ON_SECOND y remueve (disparando ON_EXPIRE) los
 * que se les acabó el tiempo. Snapshotea las claves antes de iterar porque
 * un componente (ej. AURA) puede llamar de vuelta a la API y modificar el
 * tracker en medio del tick.
 */
public class EffectTickTask extends BukkitRunnable {

    private final EffectTracker tracker;
    private final EffectComponentExecutor executor;

    public EffectTickTask(EffectTracker tracker, EffectComponentExecutor executor) {
        this.tracker = tracker;
        this.executor = executor;
    }

    @Override
    public void run() {

        Map<UUID, Map<String, ActiveEffect>> raw = tracker.rawView();

        if (raw.isEmpty()) {
            return;
        }

        for (UUID targetId : List.copyOf(raw.keySet())) {

            Map<String, ActiveEffect> effectsMap = raw.get(targetId);

            if (effectsMap == null || effectsMap.isEmpty()) {
                raw.remove(targetId);
                continue;
            }

            Entity entity = Bukkit.getEntity(targetId);

            if (!(entity instanceof LivingEntity target) || !entity.isValid()) {
                raw.remove(targetId);
                continue;
            }

            tickTarget(target, effectsMap);
        }
    }

    private void tickTarget(LivingEntity target, Map<String, ActiveEffect> effectsMap) {

        List<Map.Entry<String, ActiveEffect>> snapshot = List.copyOf(effectsMap.entrySet());
        List<String> expiredKeys = new ArrayList<>();

        for (var entry : snapshot) {

            ActiveEffect activeEffect = entry.getValue();
            boolean expired = activeEffect.decrementTick();

            executor.fireTrigger(target, activeEffect, EffectTriggerType.ON_TICK);

            if (activeEffect.ticksAlive() % 20 == 0) {
                executor.fireTrigger(target, activeEffect, EffectTriggerType.ON_SECOND);
            }

            if (expired) {
                executor.fireTrigger(target, activeEffect, EffectTriggerType.ON_EXPIRE);
                expiredKeys.add(entry.getKey());
            }
        }

        for (String key : expiredKeys) {
            effectsMap.remove(key);
        }
    }

}
