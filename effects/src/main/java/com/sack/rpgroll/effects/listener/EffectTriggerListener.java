package com.sack.rpgroll.effects.listener;

import com.sack.rpgroll.effects.core.EffectTriggerType;
import com.sack.rpgroll.effects.engine.EffectComponentExecutor;
import com.sack.rpgroll.effects.runtime.ActiveEffect;
import com.sack.rpgroll.effects.runtime.EffectTracker;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * Traduce eventos vanilla de Bukkit a triggers reactivos
 * (ON_DAMAGE_TAKEN/ON_DAMAGE_DEALT/ON_DEATH/ON_HEAL/ON_FOOD_CONSUME) sobre
 * los efectos que ya están activos en la entidad involucrada. Los triggers
 * periódicos (ON_TICK/ON_SECOND) los maneja {@code EffectTickTask} — este
 * listener solo cubre los reactivos a un evento puntual.
 */
public class EffectTriggerListener implements Listener {

    private final EffectTracker tracker;
    private final EffectComponentExecutor executor;

    public EffectTriggerListener(EffectTracker tracker, EffectComponentExecutor executor) {
        this.tracker = tracker;
        this.executor = executor;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {

        if (event.getEntity() instanceof LivingEntity victim) {
            fire(victim, EffectTriggerType.ON_DAMAGE_TAKEN);
        }

        if (event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof LivingEntity damager) {
            fire(damager, EffectTriggerType.ON_DAMAGE_DEALT);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        fire(event.getEntity(), EffectTriggerType.ON_DEATH);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent event) {

        if (event.getEntity() instanceof LivingEntity entity) {
            fire(entity, EffectTriggerType.ON_HEAL);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodConsume(PlayerItemConsumeEvent event) {
        fire(event.getPlayer(), EffectTriggerType.ON_FOOD_CONSUME);
    }

    private void fire(LivingEntity entity, EffectTriggerType trigger) {

        for (ActiveEffect activeEffect : tracker.getActive(entity)) {
            executor.fireTrigger(entity, activeEffect, trigger);
        }
    }

}
