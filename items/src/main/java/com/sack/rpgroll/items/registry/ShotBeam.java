package com.sack.rpgroll.items.registry;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El láser sostenido de {@code SHOOT} ({@code sustain} en tics): el rayo se queda encendido y sigue la
 * mira cada tic. Mantener el clic derecho repite el disparo cada 4 tics en Java, y cada disparo lo
 * renueva; al soltar se apaga cuando se acaban los tics. El daño y la munición van por disparo.
 */
final class ShotBeam implements Runnable {

    private static final Map<UUID, ShotBeam> ACTIVE = new HashMap<>();

    private final Player player;
    private final Plugin owner;
    private BukkitTask task;
    private ShotVisuals visuals;
    private double range;
    private int ticksLeft;
    private final World world;
    private final int slot;
    private ShotBeamModel.Appearance appearance;
    private ShotBeamModel model;
    private ShotMuzzle muzzle;
    private EquipmentSlot hand;
    private boolean stopped;

    private ShotBeam(Plugin owner, Player player) {
        this.owner = owner;
        this.player = player;
        this.world = player.getWorld();
        this.slot = player.getInventory() == null ? -1 : player.getInventory().getHeldItemSlot();
    }

    static void hold(Plugin plugin, Player player, ShotVisuals visuals, double range, int ticks,
                     ShotBeamModel.Appearance appearance, ShotMuzzle muzzle, EquipmentSlot hand) {
        ShotBeam beam = ACTIVE.get(player.getUniqueId());
        if (beam != null && (beam.world != player.getWorld() || beam.slot != heldSlot(player))) {
            beam.finish(); beam = null;
        }
        if (beam == null) {
            beam = new ShotBeam(plugin, player);
            ACTIVE.put(player.getUniqueId(), beam);
            try {
                beam.task = plugin.getServer().getScheduler().runTaskTimer(plugin, beam, 0L, 1L);
            } catch (RuntimeException e) {
                ACTIVE.remove(player.getUniqueId(), beam);
                throw e;
            }
        }
        beam.visuals = visuals;
        beam.muzzle = muzzle;
        beam.hand = hand;
        if (!java.util.Objects.equals(beam.appearance, appearance)) {
            if (beam.model != null) beam.model.remove();
            beam.model = appearance == null ? null : new ShotBeamModel(appearance);
            beam.appearance = appearance;
        }
        beam.range = range;
        beam.ticksLeft = ticks;
    }

    static void stop(Plugin plugin) {
        for (ShotBeam beam : java.util.List.copyOf(ACTIVE.values())) {
            if (beam.owner == plugin) {
                beam.finish();
            }
        }
    }

    @Override
    public void run() {
        if (stopped) return;
        if (ACTIVE.get(player.getUniqueId()) != this || !player.isOnline() || player.isDead()
                || player.getWorld() != world || heldSlot(player) != slot || ticksLeft-- <= 0) {
            finish();
            return;
        }
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        RayTraceResult hit = BuiltinItemActions.aim(player, eye, direction, range);
        Vector end = hit != null ? hit.getHitPosition() : eye.toVector().add(direction.clone().multiply(range));
        Location from = muzzle.at(player, hand, eye, direction);
        if (end.clone().subtract(from.toVector()).dot(direction) <= 0) from = eye;
        try {
            if (model != null) model.update(from, end);
            visuals.at(eye.toVector()).trail(world, from.toVector(), end);
        } catch (RuntimeException e) {
            finish();
            owner.getLogger().warning("SHOOT: no se pudo dibujar el láser: " + e.getMessage());
        }
    }

    private static int heldSlot(Player player) {
        return player.getInventory() == null ? -1 : player.getInventory().getHeldItemSlot();
    }

    private void finish() {
        if (stopped) return;
        stopped = true;
        if (task != null) task.cancel();
        if (model != null) model.remove();
        ACTIVE.remove(player.getUniqueId(), this);
    }
}
