package com.sack.rpgroll.disasters.engine;

import com.sack.rpgroll.disasters.core.DisasterPhenomenon;
import com.sack.rpgroll.disasters.engine.Looks.ParticleLook;
import com.sack.rpgroll.disasters.engine.Looks.SoundLook;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * El suelo tiembla bajo cada jugador: sacudidas, polvo y estruendo. Cerca de ellos se abren grietas
 * de verdad que serpentean, se ramifican y se van abriendo poco a poco, más hondas en el centro; lo
 * que esté cerca resbala hacia dentro. Bajo techo o en cuevas el techo se desprende y cae sobre quien
 * esté debajo. Lo roto se repone con el tiempo (ver {@link WorldDamage}). Es de los pocos fenómenos
 * que también ocurren en el Nether y el End.
 * <p>
 * Parámetros y valores por defecto en {@code disasters/_reference.yml}.
 */
final class Earthquake extends Phenomenon {

    /** Una columna de grieta por abrir: dónde y cuántos bloques hacia abajo. */
    private record Column(int x, int z, int depth) {
    }

    private final Deque<Column> opening = new ArrayDeque<>();
    private final List<Location> openFissures = new ArrayList<>();
    private int nextFissure;

    Earthquake(DisasterEngine engine, World world, DisasterPhenomenon params, Power power, int durationTicks) {
        super(engine, world, params, power, durationTicks, 10);
    }

    @Override
    void tick() {

        double shake = strength("shake", 0.18);
        double shakeUp = number("shake-up", 0.6);
        int dust = (int) number("ground-particles", 12);
        SoundLook rumble = sound("rumble-sound", "BLOCK_BASALT_BREAK 1 0.5");
        int boomEvery = (int) number("boom-every", 40);
        boolean boom = boomEvery > 0 && age % boomEvery < period;
        boolean fissureTime = age >= nextFissure;
        if (fissureTime) {
            nextFissure = age + (int) Math.max(period, number("fissure-interval", 200));
        }

        for (Player player : engine.targets(world)) {

            Location at = player.getLocation();
            Block below = at.getBlock().getRelative(BlockFace.DOWN);

            rumble.play(player);
            if (boom) {
                sound("boom-sound", "ENTITY_WARDEN_DIG 0.8 0.6").play(player);
            }

            if (below.getType().isSolid()) {
                player.setVelocity(player.getVelocity().add(
                        new Vector((random() - 0.5) * shake, random() * shake * shakeUp, (random() - 0.5) * shake)));
                if (dust > 0) {
                    world.spawnParticle(Particle.BLOCK, at, dust, 0.6, 0.1, 0.6, 0, below.getBlockData());
                }
            }

            if (fissureTime && random() < Math.min(1, strength("fissure-chance", 0.6))) {
                planFissure(at);
            }
            if (random() < strength("rocks", 0.12)) {
                dropRock(player);
            }
        }

        openColumns();
        pullTowardFissures();
    }

    // ----- Grietas -----

    /**
     * Traza una grieta que empieza entre {@code fissure-distance-min} y {@code -max} bloques del jugador
     * (nunca bajo sus pies) y avanza girando un poco a cada paso. Es más ancha y honda en medio y se
     * estrecha en las puntas; con {@code fissure-branch-chance} le salen ramas.
     */
    private void planFissure(Location near) {

        if (!engine.damage().enabled()) {
            return;
        }

        double angle = random() * Math.PI * 2;
        double min = number("fissure-distance-min", 5);
        double distance = min + random() * Math.max(0, number("fissure-distance-max", 14) - min);
        Location start = near.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        if (engine.inSafeZone(start)) {
            return;
        }

        Set<Long> seen = new HashSet<>();
        walk(start.getX(), start.getZ(), random() * Math.PI * 2, scaled("fissure-length", 18),
                scaled("fissure-width", 3), scaled("fissure-depth", 7), seen, 0);
        openFissures.add(start);
        sound("fissure-sound", "ENTITY_GENERIC_EXPLODE 1.5 0.5").play(world, start);
    }

    private void walk(double x, double z, double heading, double length, double width, double depth, Set<Long> seen,
            int generation) {

        double jitter = number("fissure-jitter", 0.35);
        double branch = generation < 2 ? number("fissure-branch-chance", 0.12) : 0;

        for (int step = 0; step < length; step++) {
            // Más ancha y honda en medio, fina en las puntas.
            double taper = Math.sin(Math.PI * (step + 0.5) / length);
            double half = Math.max(0.5, width * taper / 2);
            double sideX = -Math.sin(heading);
            double sideZ = Math.cos(heading);

            for (double offset = -half; offset <= half; offset += 1) {
                int bx = (int) Math.floor(x + sideX * offset);
                int bz = (int) Math.floor(z + sideZ * offset);
                if (seen.add(((long) bx << 32) ^ (bz & 0xffffffffL))) {
                    double edge = 1 - Math.abs(offset) / (half + 1);
                    opening.add(new Column(bx, bz, (int) Math.max(1, Math.round(depth * taper * edge))));
                }
            }

            if (random() < branch) {
                double side = random() < 0.5 ? -1 : 1;
                walk(x, z, heading + side * (0.7 + random() * 0.5), length * 0.5, width * 0.6, depth * 0.7, seen,
                        generation + 1);
            }

            heading += (random() - 0.5) * 2 * jitter;
            x += Math.cos(heading);
            z += Math.sin(heading);
        }
    }

    /** Abre {@code fissure-speed} columnas por paso, en el orden en que se trazaron: la grieta avanza. */
    private void openColumns() {

        WorldDamage damage = engine.damage();
        List<ParticleLook> dust = particles("fissure-particles", List.of("BLOCK*10", "CAMPFIRE_COSY_SMOKE*1"));
        int speed = (int) Math.max(1, number("fissure-speed", 6) * period);

        for (int i = 0; i < speed && !opening.isEmpty(); i++) {
            Column column = opening.poll();
            Block top = world.getHighestBlockAt(column.x(), column.z(), HeightMap.WORLD_SURFACE);
            if (top.isLiquid()) {
                continue;
            }
            int ground = world.getHighestBlockYAt(column.x(), column.z(), HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Block surface = world.getBlockAt(column.x(), ground, column.z());
            // Un árbol encima: la grieta lo rodea en vez de partir el tronco en rodajas.
            if (Tag.LOGS.isTagged(surface.getType())) {
                continue;
            }
            List<Block> blocks = new ArrayList<>();
            for (int y = Math.min(top.getY(), ground + 2); y > ground - column.depth() && y > world.getMinHeight(); y--) {
                blocks.add(world.getBlockAt(column.x(), y, column.z()));
            }
            BlockData look = surface.getBlockData();
            for (Block block : damage.filter(surface, blocks)) {
                damage.remove(block, regenerate());
            }
            Location center = surface.getLocation().add(0.5, 1, 0.5);
            for (ParticleLook particle : dust) {
                if (particle.particle() == Particle.BLOCK && particle.data() instanceof BlockData) {
                    world.spawnParticle(Particle.BLOCK, center, Math.max(1, particle.count()), 0.4, 0.3, 0.4, 0, look,
                            true);
                } else {
                    particle.spawn(world, center, 2, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }
    }

    /** Quien está al borde de una grieta recién abierta resbala hacia ella. */
    private void pullTowardFissures() {

        double pull = strength("fissure-pull", 0.12);
        double reach = scaled("fissure-length", 18) / 2 + 3;
        if (pull <= 0 || openFissures.isEmpty()) {
            return;
        }
        if (opening.isEmpty()) {
            openFissures.clear();
            return;
        }
        for (Location fissure : openFissures) {
            for (Entity entity : world.getNearbyEntities(fissure, reach, 4, reach)) {
                if (entity instanceof LivingEntity && hits(entity) && entity.isOnGround()) {
                    Vector toward = fissure.toVector().subtract(entity.getLocation().toVector()).setY(0);
                    if (toward.lengthSquared() > 0.01) {
                        entity.setVelocity(entity.getVelocity().add(toward.normalize().multiply(pull)));
                    }
                }
            }
        }
    }

    // ----- Rocas -----

    /**
     * Bajo techo, un trozo del techo cerca del jugador se desprende y le cae encima; a cielo abierto no
     * hay de dónde. Con {@code rocks-break-ceiling} el hueco queda en el techo (y se repone con el
     * tiempo); si no, cae una roca de {@code rock-blocks} o del mismo material sin romper nada.
     */
    private void dropRock(Player player) {

        Block head = player.getEyeLocation().getBlock();
        Block ceiling = null;
        int search = (int) number("ceiling-search", 8);
        for (int i = 1; i <= search; i++) {
            Block candidate = head.getRelative(BlockFace.UP, i);
            if (candidate.getType().isSolid()) {
                ceiling = candidate;
                break;
            }
        }
        if (ceiling == null) {
            return;
        }

        double scatter = number("rock-scatter", 3);
        Block piece = ceiling.getRelative((int) Math.round((random() - 0.5) * scatter), 0,
                (int) Math.round((random() - 0.5) * scatter));
        if (!piece.getType().isSolid() || !piece.getRelative(BlockFace.DOWN).isPassable()) {
            piece = ceiling;
        }

        BlockData fromList = pick(blocks("rock-blocks", List.of()));
        BlockData rock;
        if (flag("rocks-break-ceiling", true) && DisasterEngine.plainSolid(piece)
                && !engine.damage().filter(piece, List.of(piece)).isEmpty()) {
            rock = piece.getBlockData();
            engine.damage().remove(piece, regenerate());
        } else {
            rock = fromList != null ? fromList
                    : DisasterEngine.plainSolid(piece) ? piece.getBlockData() : block("rock-fallback", "COBBLESTONE");
        }

        Location from = piece.getLocation().add(0.5, piece.getType().isAir() ? 0 : -1, 0.5);
        if (from.getBlock().getType().isSolid()) {
            return;
        }

        SoundLook crash = sound("rock-sound", "BLOCK_STONE_BREAK 1 0.7");
        double radius = number("rock-radius", 1.2);
        double damage = strength("damage", 3);
        crash.play(world, from);

        engine.fallingBlock(from, rock, landed -> {
            world.spawnParticle(Particle.BLOCK, landed, 20, 0.4, 0.2, 0.4, 0, rock);
            crash.play(world, landed);
            for (Entity entity : world.getNearbyEntities(landed, radius, 1.5, radius)) {
                if (entity instanceof LivingEntity living && hits(entity)) {
                    strike(living, damage);
                }
            }
        });
    }

}
