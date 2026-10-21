package com.sack.rpgroll.disasters.engine;

import com.sack.rpgroll.disasters.core.DisasterPhenomenon;
import com.sack.rpgroll.disasters.engine.Looks.SoundLook;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * El suelo se hunde en un pozo redondo cerca de un jugador: empieza por el centro y se abre hacia
 * fuera en forma de cuenco, y lo que esté encima resbala hacia dentro. En el fondo puede quedar
 * {@code bottom-blocks} (agua, lava, magma...). Se repone con el tiempo (ver {@link WorldDamage}).
 * <p>
 * Parámetros y valores por defecto en {@code disasters/_reference.yml}.
 */
final class Sinkhole extends Phenomenon {

    private record Column(int x, int z, int depth, boolean bottom) {
    }

    private final Deque<Column> opening = new ArrayDeque<>();
    private Location current;
    private int nextHole;

    Sinkhole(DisasterEngine engine, World world, DisasterPhenomenon params, Power power, int durationTicks) {
        super(engine, world, params, power, durationTicks, 2);
    }

    @Override
    void tick() {

        if (age >= nextHole && opening.isEmpty()) {
            nextHole = age + (int) Math.max(period, number("interval", 400));
            double chance = Math.min(1, strength("chance", 0.7));
            for (Player player : engine.targets(world)) {
                if (random() < chance) {
                    plan(player.getLocation());
                    break;
                }
            }
        }

        open();
        pull();
    }

    /** Elige el sitio (entre {@code distance-min} y {@code -max} del jugador) y ordena las columnas del centro hacia fuera. */
    private void plan(Location near) {

        if (!engine.damage().enabled()) {
            return;
        }

        double angle = random() * Math.PI * 2;
        double min = number("distance-min", 6);
        double distance = min + random() * Math.max(0, number("distance-max", 18) - min);
        Location center = near.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        if (engine.inSafeZone(center)) {
            return;
        }

        double radius = scaled("radius", 4);
        double depth = scaled("depth", 10);
        int reach = (int) Math.ceil(radius);
        List<Column> columns = new ArrayList<>();
        for (int x = -reach; x <= reach; x++) {
            for (int z = -reach; z <= reach; z++) {
                double ratio = Math.sqrt(x * x + z * z) / radius;
                if (ratio <= 1) {
                    // Cuenco: hondo en medio y casi a ras en el borde.
                    int columnDepth = (int) Math.max(1, Math.round(depth * (1 - ratio * ratio)));
                    columns.add(new Column(center.getBlockX() + x, center.getBlockZ() + z, columnDepth, ratio < 0.5));
                }
            }
        }
        columns.sort(Comparator.comparingDouble(column -> Math.hypot(column.x() - center.getBlockX(),
                column.z() - center.getBlockZ())));
        opening.addAll(columns);

        current = center;
        current.setY(world.getHighestBlockYAt(center.getBlockX(), center.getBlockZ(), HeightMap.MOTION_BLOCKING_NO_LEAVES));
        sound("sound", "BLOCK_ROOTED_DIRT_BREAK 2 0.4").play(world, current);
    }

    /** Abre {@code speed} columnas por paso: se ve hundirse desde el centro. */
    private void open() {

        WorldDamage damage = engine.damage();
        List<BlockData> bottom = blocks("bottom-blocks", List.of());
        SoundLook crumble = sound("crumble-sound", "BLOCK_GRAVEL_BREAK 1 0.6");
        int speed = (int) Math.max(1, number("speed", 2) * period);

        for (int i = 0; i < speed && !opening.isEmpty(); i++) {
            Column column = opening.poll();
            int ground = world.getHighestBlockYAt(column.x(), column.z(), HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Block surface = world.getBlockAt(column.x(), ground, column.z());
            if (surface.isLiquid() || Tag.LOGS.isTagged(surface.getType())) {
                continue;
            }

            Block top = world.getHighestBlockAt(column.x(), column.z(), HeightMap.WORLD_SURFACE);
            List<Block> blocks = new ArrayList<>();
            for (int y = Math.min(top.getY(), ground + 2); y > ground - column.depth() && y > world.getMinHeight(); y--) {
                blocks.add(world.getBlockAt(column.x(), y, column.z()));
            }
            BlockData look = surface.getBlockData();
            List<Block> removed = damage.filter(surface, blocks);
            removed.forEach(block -> damage.remove(block, regenerate()));

            if (column.bottom() && !bottom.isEmpty() && removed.size() == blocks.size()) {
                Block floor = world.getBlockAt(column.x(), ground - column.depth() + 1, column.z());
                if (damage.canPlace(floor)) {
                    damage.change(floor, pick(bottom), regenerate());
                }
            }

            Location at = surface.getLocation().add(0.5, 1, 0.5);
            world.spawnParticle(Particle.BLOCK, at, 8, 0.4, 0.4, 0.4, 0, look, true);
            if (random() < 0.3) {
                crumble.play(world, at);
            }
        }
    }

    /** Lo que esté encima del pozo mientras se abre resbala hacia el centro. */
    private void pull() {

        if (current == null) {
            return;
        }
        if (opening.isEmpty()) {
            current = null;
            return;
        }

        double strength = strength("pull", 0.2);
        double reach = scaled("radius", 4) + 2;
        for (Entity entity : world.getNearbyEntities(current, reach, 6, reach)) {
            if (entity instanceof LivingEntity && hits(entity)) {
                Vector toward = current.toVector().subtract(entity.getLocation().toVector()).setY(0);
                if (toward.lengthSquared() > 0.25) {
                    entity.setVelocity(entity.getVelocity().add(toward.normalize().multiply(strength)));
                }
            }
        }
    }

}
