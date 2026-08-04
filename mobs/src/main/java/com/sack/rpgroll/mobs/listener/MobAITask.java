package com.sack.rpgroll.mobs.listener;

import com.sack.rpgroll.mobs.core.MobDefinition;
import com.sack.rpgroll.mobs.core.MobManager;
import com.sack.rpgroll.mobs.core.MobTrigger;
import com.sack.rpgroll.mobs.engine.ActiveMobState;
import com.sack.rpgroll.mobs.engine.MobEngine;
import com.sack.rpgroll.mobs.instance.MobInstanceService;
import com.sack.rpgroll.mobs.region.MobRegionManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Motor de comportamiento por polling — corre cada {@code MobsPlugin.AI_TICK_INTERVAL}.
 * Bukkit/Paper no expone el sistema real de pathfinding-goals de Minecraft
 * (interno de NMS), así que esto se simula con {@link Mob#getPathfinder()}
 * / {@link Mob#setTarget(LivingEntity)} —API estable— manejados a mano:
 * los mobs corren con {@code setAware(false)} y esta tarea es su única IA.
 * Los goals se evalúan EN ORDEN, el primero que aplica gana ese tick.
 */
public class MobAITask implements Runnable {

    private static final double MELEE_RANGE = 2.2;
    private static final long PERIODIC_INTERVAL_MILLIS = 3000;

    private final MobEngine engine;
    private final MobInstanceService instanceService;
    private final MobRegionManager regionManager;
    private final MobManager mobManager;

    public MobAITask(MobEngine engine, MobInstanceService instanceService, MobRegionManager regionManager,
            MobManager mobManager) {
        this.engine = engine;
        this.instanceService = instanceService;
        this.regionManager = regionManager;
        this.mobManager = mobManager;
    }

    @Override
    public void run() {

        for (UUID entityId : List.copyOf(engine.getActiveMobs().keySet())) {
            tickMob(entityId);
        }
    }

    private void tickMob(UUID entityId) {

        Entity entity = Bukkit.getEntity(entityId);

        if (!(entity instanceof LivingEntity mob) || mob.isDead() || !(mob instanceof Mob bukkitMob)) {
            return;
        }

        Optional<String> definitionIdOpt = instanceService.getDefinitionId(mob);
        Optional<ActiveMobState> stateOpt = engine.getState(entityId);

        if (definitionIdOpt.isEmpty() || stateOpt.isEmpty()) {
            return;
        }

        MobDefinition definition = mobManager.get(definitionIdOpt.get()).orElse(null);
        if (definition == null) {
            return;
        }

        ActiveMobState state = stateOpt.get();

        tickPeriodic(mob, definition, state);
        tickPlayerRange(mob, definition, state);
        tickGoals(mob, bukkitMob, definition, state);
    }

    private void tickPeriodic(LivingEntity mob, MobDefinition definition, ActiveMobState state) {

        if (!state.isPeriodicDue(PERIODIC_INTERVAL_MILLIS)) {
            return;
        }

        state.markPeriodicFired();
        engine.runSkillsFor(MobTrigger.PERIODIC, mob, definition, state, findNearestTarget(mob, definition));
        engine.runSkillsFor(MobTrigger.HEALTH_THRESHOLD, mob, definition, state, findNearestTarget(mob, definition));
    }

    private void tickPlayerRange(LivingEntity mob, MobDefinition definition, ActiveMobState state) {

        double range = definition.ai().aggroRange();

        List<Player> nearby = mob.getWorld().getPlayers().stream()
                .filter(player -> player.getLocation().distanceSquared(mob.getLocation()) <= range * range)
                .toList();

        for (Player player : nearby) {
            if (state.playersInRange().add(player.getUniqueId())) {
                engine.runSkillsFor(MobTrigger.PLAYER_ENTER_RANGE, mob, definition, state, player);
            }
        }

        state.playersInRange().removeIf(id -> nearby.stream().noneMatch(p -> p.getUniqueId().equals(id)));
    }

    private void tickGoals(LivingEntity mob, Mob bukkitMob, MobDefinition definition, ActiveMobState state) {

        for (String goal : definition.ai().goals()) {

            if (runGoal(goal.toUpperCase(Locale.ROOT), mob, bukkitMob, definition, state)) {
                return;
            }
        }
    }

    private boolean runGoal(String goal, LivingEntity mob, Mob bukkitMob, MobDefinition definition,
            ActiveMobState state) {

        return switch (goal) {
            case "FLEE" -> tryFlee(mob, bukkitMob, definition);
            case "ATTACK_PLAYERS" -> tryAttack(mob, bukkitMob, definition, state);
            case "DEFEND_ALLY" -> tryDefendAlly(mob, bukkitMob, definition, state);
            case "GUARD_REGION" -> tryGuardRegion(mob, bukkitMob, definition);
            case "FOLLOW_LEADER" -> tryFollowLeader(mob, bukkitMob, definition);
            case "PATROL" -> tryPatrol(mob, bukkitMob, definition, state);
            case "STAY_STILL" -> true;
            default -> false;
        };
    }

    private boolean tryFlee(LivingEntity mob, Mob bukkitMob, MobDefinition definition) {

        double fleeThreshold = definition.ai().fleeHealthPercent();
        if (fleeThreshold <= 0 || bukkitMob.getTarget() == null) {
            return false;
        }

        var maxHealthAttr = mob.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
        double healthPercent = maxHealth <= 0 ? 0 : (mob.getHealth() / maxHealth) * 100.0;

        if (healthPercent > fleeThreshold) {
            return false;
        }

        LivingEntity target = bukkitMob.getTarget();
        Location away = mob.getLocation().add(mob.getLocation().toVector().subtract(target.getLocation().toVector())
                .normalize().multiply(8));

        bukkitMob.getPathfinder().moveTo(away, definition.ai().moveSpeed());
        return true;
    }

    private boolean tryAttack(LivingEntity mob, Mob bukkitMob, MobDefinition definition, ActiveMobState state) {

        LivingEntity target = findNearestTarget(mob, definition);
        if (target == null) {
            if (bukkitMob.getTarget() != null) {
                engine.runSkillsFor(MobTrigger.LOSE_TARGET, mob, definition, state, null);
                bukkitMob.setTarget(null);
            }
            return false;
        }

        bukkitMob.setTarget(target);

        double distanceSquared = mob.getLocation().distanceSquared(target.getLocation());

        if (distanceSquared <= MELEE_RANGE * MELEE_RANGE) {

            double damage = engine.rollAttackDamage(definition);
            if (damage > 0) {
                target.damage(damage, mob);
            }

            engine.runSkillsFor(MobTrigger.ATTACK, mob, definition, state, target);
        } else {
            bukkitMob.getPathfinder().moveTo(target, definition.ai().moveSpeed());
        }

        return true;
    }

    private boolean tryDefendAlly(LivingEntity mob, Mob bukkitMob, MobDefinition definition, ActiveMobState state) {

        for (Entity nearby : mob.getNearbyEntities(16, 16, 16)) {

            if (!(nearby instanceof Mob allyMob) || !(nearby instanceof LivingEntity allyLiving)) {
                continue;
            }

            if (instanceService.getDefinitionId(allyLiving).filter(id -> id.equals(definition.id())).isEmpty()) {
                continue;
            }

            if (allyMob.getTarget() instanceof Player attacker) {
                bukkitMob.setTarget(attacker);
                bukkitMob.getPathfinder().moveTo(attacker, definition.ai().moveSpeed());
                return true;
            }
        }

        return false;
    }

    private boolean tryGuardRegion(LivingEntity mob, Mob bukkitMob, MobDefinition definition) {

        String regionId = definition.ai().guardRegionId();
        if (regionId == null) {
            return false;
        }

        var region = regionManager.get(regionId);
        if (region.isEmpty()) {
            return false;
        }

        if (!region.get().contains(mob.getLocation())) {
            bukkitMob.setTarget(null);
            bukkitMob.getPathfinder().moveTo(region.get().center(), definition.ai().moveSpeed());
            return true;
        }

        return false;
    }

    private boolean tryFollowLeader(LivingEntity mob, Mob bukkitMob, MobDefinition definition) {

        for (Entity nearby : mob.getNearbyEntities(20, 20, 20)) {

            if (!(nearby instanceof LivingEntity ally) || ally.equals(mob)) {
                continue;
            }

            if (instanceService.getDefinitionId(ally).filter(id -> id.equals(definition.id())).isPresent()) {
                bukkitMob.getPathfinder().moveTo(ally, definition.ai().moveSpeed());
                return true;
            }
        }

        return false;
    }

    private boolean tryPatrol(LivingEntity mob, Mob bukkitMob, MobDefinition definition, ActiveMobState state) {

        List<String> points = definition.ai().patrolPoints();
        if (points.isEmpty()) {
            return false;
        }

        String[] parts = points.get(state.patrolIndex() % points.size()).split(",");
        if (parts.length != 3) {
            return false;
        }

        try {
            Location target = new Location(mob.getWorld(), Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim()), Double.parseDouble(parts[2].trim()));

            if (mob.getLocation().distanceSquared(target) <= 4) {
                state.advancePatrol(points.size());
            } else {
                bukkitMob.getPathfinder().moveTo(target, definition.ai().moveSpeed());
            }

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private LivingEntity findNearestTarget(LivingEntity mob, MobDefinition definition) {

        double range = definition.ai().aggroRange();

        return mob.getWorld().getPlayers().stream()
                .filter(player -> player.getGameMode() == org.bukkit.GameMode.SURVIVAL
                        || player.getGameMode() == org.bukkit.GameMode.ADVENTURE)
                .filter(player -> player.getLocation().distanceSquared(mob.getLocation()) <= range * range)
                .min((a, b) -> Double.compare(
                        a.getLocation().distanceSquared(mob.getLocation()),
                        b.getLocation().distanceSquared(mob.getLocation())))
                .map(LivingEntity.class::cast)
                .orElse(null);
    }

}
