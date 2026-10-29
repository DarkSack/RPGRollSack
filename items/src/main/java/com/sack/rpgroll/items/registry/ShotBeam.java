package com.sack.rpgroll.items.registry;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * El láser sostenido de {@code SHOOT} ({@code sustain} en tics): el rayo se queda encendido y sigue la
 * mira cada tic. Mantener el clic derecho repite el disparo cada 4 tics en Java, y cada disparo lo
 * renueva; al soltar se apaga cuando se acaban los tics. El daño y la munición van por disparo.
 */
final class ShotBeam implements Consumer<BukkitTask> {

    private static final Map<UUID, ShotBeam> ACTIVE = new HashMap<>();

    private final Player player;
    private ShotVisuals visuals;
    private double range;
    private int ticksLeft;

    private ShotBeam(Player player) {
        this.player = player;
    }

    static void hold(Plugin plugin, Player player, ShotVisuals visuals, double range, int ticks) {
        ShotBeam beam = ACTIVE.get(player.getUniqueId());
        if (beam == null) {
            beam = new ShotBeam(player);
            ACTIVE.put(player.getUniqueId(), beam);
            plugin.getServer().getScheduler().runTaskTimer(plugin, beam, 0L, 1L);
        }
        beam.visuals = visuals;
        beam.range = range;
        beam.ticksLeft = ticks;
    }

    @Override
    public void accept(BukkitTask task) {
        if (!player.isOnline() || player.isDead() || ticksLeft-- <= 0) {
            task.cancel();
            ACTIVE.remove(player.getUniqueId(), this);
            return;
        }
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        RayTraceResult hit = BuiltinItemActions.aim(player, eye, direction, range);
        Vector end = hit != null ? hit.getHitPosition() : eye.toVector().add(direction.clone().multiply(range));
        visuals.at(eye.toVector()).trail(player.getWorld(), BuiltinItemActions.barrel(eye, direction).toVector(), end);
    }
}
