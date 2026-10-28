package com.sack.rpgroll.items.registry;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * El proyectil que vuela de {@code SHOOT} con {@code speed}: avanza cada tick, con gravedad si la
 * tiene, y pega en la primera criatura o bloque de su tramo. Se ve con un ItemDisplay del
 * {@code model} (en Bedrock solo si Geyser tiene la extensión de display entities) y con la estela
 * de partículas, que es lo que ven todos.
 */
final class ShotProjectile extends BukkitRunnable {

    private final Player shooter;
    private final World world;
    private final Location at;
    private final Vector velocity;
    private final double gravity;
    private final int maxTicks;
    private final ShotVisuals visuals;
    private final ShotImpact impact;
    private final ItemDisplay display;
    private int ticks;

    private ShotProjectile(Player shooter, Location from, Vector velocity, double gravity, double range,
                           String model, ShotVisuals visuals, ShotImpact impact) {
        this.shooter = shooter;
        this.world = from.getWorld();
        this.at = from.clone();
        this.velocity = velocity;
        this.gravity = gravity;
        this.maxTicks = (int) Math.ceil(range / velocity.length());
        this.visuals = visuals;
        this.impact = impact;
        this.display = model.isBlank() ? null : spawnDisplay(model);
    }

    static void launch(Plugin plugin, Player shooter, Location from, Vector direction, double speed, double gravity,
                       double range, String model, ShotVisuals visuals, ShotImpact impact) {
        new ShotProjectile(shooter, from, direction.clone().normalize().multiply(speed), gravity, range, model,
                visuals, impact).runTaskTimer(plugin, 1L, 1L);
    }

    @Override
    public void run() {
        if (!shooter.isOnline() || ticks++ >= maxTicks || !at.isChunkLoaded()) {
            finish(null, null);
            return;
        }
        double length = velocity.length();
        RayTraceResult hit = world.rayTrace(at, velocity, length, FluidCollisionMode.NEVER, true, 0.3,
                entity -> entity instanceof LivingEntity && !entity.equals(shooter));
        Vector end = hit != null ? hit.getHitPosition() : at.toVector().add(velocity);
        visuals.trail(world, at.toVector(), end);
        if (hit != null) {
            finish(end, hit.getHitEntity() instanceof LivingEntity living ? living : null);
            return;
        }
        at.add(velocity);
        velocity.setY(velocity.getY() - gravity);
        if (display != null) {
            display.teleport(oriented(at, velocity));
        }
    }

    /** Sin {@code end} se ha quedado sin alcance: desaparece sin impacto. */
    private void finish(Vector end, LivingEntity target) {
        cancel();
        if (display != null) {
            display.remove();
        }
        if (end != null) {
            impact.apply(shooter, end.toLocation(world), target);
        }
    }

    private ItemDisplay spawnDisplay(String model) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setItemModel(NamespacedKey.fromString(model));
        item.setItemMeta(meta);
        return world.spawn(oriented(at, velocity), ItemDisplay.class, d -> {
            d.setItemStack(item);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setTeleportDuration(1);
            d.setPersistent(false);
        });
    }

    /** El modelo se construye con la punta hacia +z, que el display gira hacia donde vuela. */
    private static Location oriented(Location location, Vector direction) {
        Location copy = location.clone();
        copy.setDirection(direction);
        return copy;
    }
}
