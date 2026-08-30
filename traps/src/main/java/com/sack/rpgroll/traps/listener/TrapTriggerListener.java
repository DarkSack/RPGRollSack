package com.sack.rpgroll.traps.listener;

import com.sack.rpgroll.traps.core.TrapDefinition;
import com.sack.rpgroll.traps.core.TrapState;
import com.sack.rpgroll.traps.core.TrapTrigger;
import com.sack.rpgroll.traps.core.TrapManager;
import com.sack.rpgroll.traps.engine.TrapEngine;
import com.sack.rpgroll.traps.engine.TriggerContext;
import com.sack.rpgroll.traps.location.PlacedTrap;
import com.sack.rpgroll.traps.location.PlacedTrapManager;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.List;
import java.util.Optional;

/**
 * Escucha los eventos vanilla que corresponden a los triggers dirigidos por
 * evento (todos salvo PROXIMITY/LINE_OF_SIGHT/TIMER, que resuelve el tick
 * periódico del motor — ver {@link TrapEngine}). Cada handler solo busca
 * instancias colocadas cuya posición/zona coincide y delega la decisión de
 * disparar (condiciones, cooldown, cargas) al motor.
 */
public class TrapTriggerListener implements Listener {

    private final TrapManager trapManager;
    private final PlacedTrapManager placedTrapManager;
    private final TrapEngine engine;

    public TrapTriggerListener(TrapManager trapManager, PlacedTrapManager placedTrapManager, TrapEngine engine) {
        this.trapManager = trapManager;
        this.placedTrapManager = placedTrapManager;
        this.engine = engine;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {

        if (samePosition(event.getFrom(), event.getTo())) {
            return;
        }

        Player player = event.getPlayer();
        Location feet = event.getTo();
        Location feetBelow = feet.clone().subtract(0, 1, 0);

        for (PlacedTrap placed : matchingTrigger(TrapTrigger.PRESSURE)) {
            if (placed.contains(feetBelow)) {
                engine.tryTrigger(placed, TriggerContext.of(player));
            }
        }

        for (PlacedTrap placed : matchingTrigger(TrapTrigger.MOVEMENT)) {
            if (withinTrap(placed, feet)) {
                engine.tryTrigger(placed, TriggerContext.of(player));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityMove(EntityMoveEvent event) {

        if (event.getEntity() instanceof Player) {
            return; // los jugadores ya se manejan en onMove (MOVEMENT/PRESSURE)
        }

        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        for (PlacedTrap placed : matchingTrigger(TrapTrigger.ENTITY)) {
            if (withinTrap(placed, event.getTo())) {
                engine.tryTrigger(placed, TriggerContext.none());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {

        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        for (PlacedTrap placed : matchingTrigger(TrapTrigger.INTERACTION)) {
            if (placed.contains(clicked.getLocation())) {
                engine.tryTrigger(placed, TriggerContext.of(event.getPlayer()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {

        for (PlacedTrap placed : matchingTrigger(TrapTrigger.BLOCK_BREAK)) {
            if (placed.contains(event.getBlock().getLocation())) {
                engine.tryTrigger(placed, TriggerContext.of(event.getPlayer()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        for (PlacedTrap placed : matchingTrigger(TrapTrigger.BLOCK_PLACE)) {
            if (placed.contains(event.getBlock().getLocation())) {
                engine.tryTrigger(placed, TriggerContext.of(event.getPlayer()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {

        Location hitLocation = event.getHitBlock() != null
                ? event.getHitBlock().getLocation()
                : event.getEntity().getLocation();

        Player shooter = event.getEntity().getShooter() instanceof Player p ? p : null;

        for (PlacedTrap placed : matchingTrigger(TrapTrigger.PROJECTILE)) {
            if (placed.contains(hitLocation)) {
                engine.tryTrigger(placed, TriggerContext.of(shooter));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent event) {

        if (event.getOldCurrent() > 0 || event.getNewCurrent() <= 0) {
            return; // solo flanco de subida (0 -> algo) — evita reintentar en cada tick de señal sostenida
        }

        for (PlacedTrap placed : matchingTrigger(TrapTrigger.REDSTONE)) {
            if (placed.contains(event.getBlock().getLocation())) {
                engine.tryTrigger(placed, TriggerContext.none());
            }
        }
    }

    private boolean withinTrap(PlacedTrap placed, Location location) {

        if (placed.isZone()) {
            return placed.contains(location);
        }

        Optional<TrapDefinition> definitionOpt = trapManager.get(placed.trapId());
        return definitionOpt.isPresent() && location.getWorld() != null
                && location.getWorld().getName().equals(placed.world())
                && location.distance(placed.primaryLocation()) <= definitionOpt.get().radius();
    }

    private boolean samePosition(Location a, Location b) {
        return a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }

    private List<PlacedTrap> matchingTrigger(TrapTrigger trigger) {
        return placedTrapManager.getAll().stream()
                .filter(placed -> placed.state() == TrapState.ARMED)
                .filter(placed -> trapManager.get(placed.trapId())
                        .map(TrapDefinition::trigger)
                        .map(t -> t == trigger)
                        .orElse(false))
                .toList();
    }

}
