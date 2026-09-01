package com.sack.rpgroll.traps.engine;

import com.sack.rpgroll.traps.condition.TrapConditionContext;
import com.sack.rpgroll.traps.condition.TrapConditionEvaluator;
import com.sack.rpgroll.traps.core.TrapDefinition;
import com.sack.rpgroll.traps.core.TrapManager;
import com.sack.rpgroll.traps.core.TrapState;
import com.sack.rpgroll.traps.core.TrapTrigger;
import com.sack.rpgroll.traps.location.PlacedTrap;
import com.sack.rpgroll.traps.location.PlacedTrapManager;
import com.sack.rpgroll.traps.registry.TrapActionContext;
import com.sack.rpgroll.traps.registry.TrapActionRegistry;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Motor central: decide si una instancia colocada puede dispararse, ejecuta
 * sus acciones y condiciones, avanza su state machine y persiste el
 * resultado. También corre el tick periódico que re-evalúa los triggers sin
 * evento vanilla (PROXIMITY/LINE_OF_SIGHT/TIMER) y expira cooldowns.
 * <p>
 * Nota de diseño: para TIMER se reusa {@code cooldownMillis} como el
 * período del temporizador (en vez de un reloj paralelo) — una trampa TIMER
 * simplemente reintenta dispararse en cada tick mientras está ARMED, y el
 * propio cooldown post-disparo ya define cada cuánto vuelve a hacerlo. El
 * campo {@code trigger.params.interval-seconds} queda como documentación
 * para el admin (usarlo para fijar el cooldown de la trampa), no como un
 * segundo reloj independiente — evita duplicar estado sin necesidad.
 */
public class TrapEngine {

    private final Plugin plugin;
    private final TrapManager trapManager;
    private final PlacedTrapManager placedTrapManager;
    private final TrapActionRegistry actionRegistry;
    private final TrapConditionEvaluator conditionEvaluator;

    public TrapEngine(Plugin plugin, TrapManager trapManager, PlacedTrapManager placedTrapManager,
            TrapActionRegistry actionRegistry, TrapConditionEvaluator conditionEvaluator) {
        this.plugin = plugin;
        this.trapManager = trapManager;
        this.placedTrapManager = placedTrapManager;
        this.actionRegistry = actionRegistry;
        this.conditionEvaluator = conditionEvaluator;
    }

    /** Intenta disparar una instancia ya resuelta — usado por los listeners de evento. */
    public boolean tryTrigger(PlacedTrap placed, TriggerContext ctx) {

        if (placed.state() != TrapState.ARMED) {
            return false;
        }

        Optional<TrapDefinition> definitionOpt = trapManager.get(placed.trapId());
        if (definitionOpt.isEmpty()) {
            return false;
        }

        TrapDefinition definition = definitionOpt.get();

        if (!conditionEvaluator.evaluateAll(definition.conditions(), new TrapConditionContext(ctx.triggeringPlayer()))) {
            return false;
        }

        execute(placed, definition, ctx);
        return true;
    }

    /**
     * Fuerza el disparo de una instancia por su placementId, ignorando quién
     * la llamó — usado por cadenas (TRIGGER_TRAP) y por
     * {@code /trapadmin forcetrigger}. Sigue respetando estado/condiciones/
     * cooldown/cargas de la trampa objetivo, no las salta.
     */
    public boolean forceTrigger(String placementId, Player triggeringPlayer) {

        Optional<PlacedTrap> placedOpt = placedTrapManager.get(placementId);
        if (placedOpt.isEmpty()) {
            plugin.getLogger().warning("✘ forceTrigger: no existe la instancia '" + placementId + "'.");
            return false;
        }

        return tryTrigger(placedOpt.get(), TriggerContext.of(triggeringPlayer));
    }

    private void execute(PlacedTrap placed, TrapDefinition definition, TriggerContext ctx) {

        PlacedTrap triggered = placed.withState(TrapState.TRIGGERED);
        placedTrapManager.update(triggered);

        TrapActionContext actionContext = new TrapActionContext(triggered, definition, ctx.triggeringPlayer(), this,
                null, null);
        actionRegistry.executeAll(definition.actions(), actionContext);

        applyDisguiseReveal(triggered, definition);

        for (String chainedId : definition.chain()) {
            chainTargets(chainedId).forEach(target -> tryTrigger(target, ctx));
        }

        int chargesRemaining = definition.hasInfiniteCharges() ? -1 : Math.max(0, triggered.chargesRemaining() - 1);

        if (!definition.hasInfiniteCharges() && chargesRemaining <= 0) {
            placedTrapManager.update(triggered.withCharges(0).withState(TrapState.DEPLETED));
            return;
        }

        if (definition.cooldownMillis() > 0) {
            long expiresAt = System.currentTimeMillis() + definition.cooldownMillis();
            placedTrapManager.update(triggered.withCharges(chargesRemaining).withState(TrapState.COOLDOWN)
                    .withCooldownExpiresAt(expiresAt));
        } else {
            // Vuelve a ARMED directamente (sin pasar por COOLDOWN) — a diferencia de tick(),
            // que restaura el disguise al expirar el cooldown, este camino se lo saltaba y el
            // bloque se quedaba en revealedMaterial para siempre en trampas sin cooldown.
            restoreDisguise(triggered, definition);
            placedTrapManager.update(triggered.withCharges(chargesRemaining).withState(TrapState.ARMED));
        }
    }

    /** Instancias colocadas cuyo tipo coincide con un id de la cadena. */
    private List<PlacedTrap> chainTargets(String chainedTrapId) {

        List<PlacedTrap> targets = new ArrayList<>();

        for (PlacedTrap placed : placedTrapManager.getAll()) {
            if (placed.trapId().equalsIgnoreCase(chainedTrapId)) {
                targets.add(placed);
            }
        }

        return targets;
    }

    private void applyDisguiseReveal(PlacedTrap placed, TrapDefinition definition) {

        if (!definition.disguise().isActive() || definition.disguise().revealedMaterial() == null) {
            return;
        }

        Location location = placed.primaryLocation();
        if (location != null) {
            location.getBlock().setType(definition.disguise().revealedMaterial());
        }
    }

    private void restoreDisguise(PlacedTrap placed, TrapDefinition definition) {

        if (!definition.disguise().isActive()) {
            return;
        }

        Location location = placed.primaryLocation();
        if (location != null) {
            location.getBlock().setType(definition.disguise().visibleMaterial());
        }
    }

    /** @param intervalTicks intervalo del tick periódico, leído de config.yml ("engine.tick-interval-ticks"). */
    public void start(long intervalTicks) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    private void tick() {

        long now = System.currentTimeMillis();
        List<PlacedTrap> snapshot = List.copyOf(placedTrapManager.getAll());

        for (PlacedTrap placed : snapshot) {

            Optional<TrapDefinition> definitionOpt = trapManager.get(placed.trapId());
            if (definitionOpt.isEmpty()) {
                continue;
            }

            TrapDefinition definition = definitionOpt.get();

            if (placed.state() == TrapState.COOLDOWN && placed.cooldownExpiresAtMillis() <= now) {
                PlacedTrap rearmed = placed.withState(TrapState.ARMED);
                placedTrapManager.update(rearmed);
                restoreDisguise(rearmed, definition);
                placed = rearmed;
            }

            if (placed.state() != TrapState.ARMED || !definition.trigger().isPolled()) {
                continue;
            }

            PlacedTrap current = placed;

            switch (definition.trigger()) {
                case TIMER -> tryTrigger(current, TriggerContext.none());
                case PROXIMITY -> findNearbyPlayer(current, definition).ifPresent(
                        player -> tryTrigger(current, TriggerContext.of(player)));
                case LINE_OF_SIGHT -> findVisiblePlayer(current, definition).ifPresent(
                        player -> tryTrigger(current, TriggerContext.of(player)));
                default -> {
                }
            }
        }
    }

    private Optional<Player> findNearbyPlayer(PlacedTrap placed, TrapDefinition definition) {

        Location anchor = placed.primaryLocation();
        if (anchor == null) {
            return Optional.empty();
        }

        for (Player player : anchor.getWorld().getPlayers()) {
            if (placed.isZone() ? placed.contains(player.getLocation())
                    : player.getLocation().distance(anchor) <= definition.radius()) {
                return Optional.of(player);
            }
        }

        return Optional.empty();
    }

    private Optional<Player> findVisiblePlayer(PlacedTrap placed, TrapDefinition definition) {

        Location anchor = placed.primaryLocation();
        if (anchor == null) {
            return Optional.empty();
        }

        Location eye = anchor.clone().add(0.5, 0.5, 0.5);

        for (Player player : anchor.getWorld().getPlayers()) {

            if (player.getLocation().distance(anchor) > definition.radius()) {
                continue;
            }

            RayTraceResult trace = anchor.getWorld().rayTraceBlocks(eye, player.getEyeLocation().toVector()
                    .subtract(eye.toVector()).normalize(), definition.radius(), FluidCollisionMode.NEVER, true);

            if (trace == null || trace.getHitBlock() == null) {
                return Optional.of(player);
            }
        }

        return Optional.empty();
    }


    /** El plugin dueño del motor, para acciones que necesitan programar tareas. */
    public Plugin getPlugin() {
        return plugin;
    }

}
