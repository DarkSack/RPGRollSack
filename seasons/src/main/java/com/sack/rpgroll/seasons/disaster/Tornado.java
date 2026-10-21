package com.sack.rpgroll.seasons.disaster;

import com.sack.rpgroll.seasons.core.DisasterPhenomenon;
import com.sack.rpgroll.seasons.disaster.Looks.ParticleLook;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Un embudo que nace lejos de un jugador y va hacia los jugadores. Arrastra en espiral a quien
 * pilla, lo sube por el centro y lo lanza hacia fuera; alrededor giran escombros. Con
 * {@code break-blocks} arranca la vegetación natural por donde pasa (hierba, flores, hojas de
 * árboles) y la repone al rato. Con otras partículas y bloques sirve para un remolino de fuego,
 * de arena o de nieve.
 * <p>
 * Parámetros y valores por defecto en {@code disasters/_reference.yml}.
 */
final class Tornado extends Phenomenon {

    /** Vegetación natural: lo que un tornado arranca sin dejar el mundo hecho un desastre. */
    private static final List<String> BREAKABLE = List.of("SHORT_GRASS", "TALL_GRASS", "FERN", "LARGE_FERN",
            "DEAD_BUSH", "BUSH", "SHORT_DRY_GRASS", "TALL_DRY_GRASS", "LEAF_LITTER", "FIREFLY_BUSH", "WILDFLOWERS",
            "VINE", "SNOW", "DANDELION", "POPPY", "BLUE_ORCHID", "ALLIUM", "AZURE_BLUET", "OXEYE_DAISY", "CORNFLOWER",
            "*_TULIP", "SUNFLOWER", "LILAC", "ROSE_BUSH", "PEONY", "*_LEAVES");

    private final List<FallingBlock> debris = new ArrayList<>();
    private final Map<UUID, Integer> flungUntil = new HashMap<>();
    private Location center;
    private Location goal;

    Tornado(DisasterEngine engine, World world, DisasterPhenomenon params, double intensity, int durationTicks) {
        super(engine, world, params, intensity, durationTicks, 2);
    }

    @Override
    boolean defaultStorm() {
        return true;
    }

    @Override
    void tick() {

        if (center == null && !spawn()) {
            engine.finished(this);
            return;
        }

        move();

        double height = number("height", 26);
        drawFunnel(height);
        pull(height);
        rip(height);
        spinDebris(height);

        int soundEvery = (int) number("sound-every", 30);
        if (soundEvery > 0 && age % soundEvery < period) {
            sound("sound", "ITEM_ELYTRA_FLYING 4 0.5").play(world, center);
        }
    }

    /** Nace entre {@code spawn-distance-min} y {@code -max} bloques de un jugador al azar, fuera de la zona segura. */
    private boolean spawn() {

        List<Player> targets = engine.targets(world);
        if (targets.isEmpty()) {
            return false;
        }

        double min = number("spawn-distance-min", 30);
        double max = Math.max(min, number("spawn-distance-max", 45));

        for (int attempt = 0; attempt < 8; attempt++) {
            Location near = pick(targets).getLocation();
            double angle = random() * Math.PI * 2;
            double distance = min + random() * (max - min);
            Location candidate = ground(near.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance));
            if (!engine.inSafeZone(candidate)) {
                center = candidate;
                return true;
            }
        }
        return false;
    }

    private Location ground(Location location) {
        location.setY(world.getHighestBlockYAt(location.getBlockX(), location.getBlockZ(), HeightMap.MOTION_BLOCKING) + 1);
        return location;
    }

    /** Avanza hacia un jugador (desviándose hasta {@code wander} bloques) y cambia de objetivo cada {@code retarget-ticks}. */
    private void move() {

        int retarget = (int) Math.max(period, number("retarget-ticks", 200));
        if (goal == null || age % retarget < period) {
            List<Player> targets = engine.targets(world);
            double wander = number("wander", 20);
            goal = targets.isEmpty() ? null
                    : pick(targets).getLocation().add((random() - 0.5) * wander, 0, (random() - 0.5) * wander);
        }

        if (goal == null) {
            return;
        }

        Vector step = goal.toVector().subtract(center.toVector()).setY(0);
        double distance = step.length();
        if (distance < 1) {
            return;
        }

        double advance = Math.min(distance, number("speed", 0.2) * period);
        Location next = ground(center.clone().add(step.multiply(advance / distance)));
        if (engine.inSafeZone(next)) {
            goal = null;
            return;
        }
        center = next;
    }

    /** Un brazo en espiral por cada partícula de {@code funnel-particles}. */
    private void drawFunnel(double height) {

        List<ParticleLook> arms = particles("funnel-particles", List.of("CLOUD", "LARGE_SMOKE", "DUST #6B6B6B 3", "CLOUD", "LARGE_SMOKE"));
        double base = number("funnel-base-radius", 0.6);
        double growth = number("funnel-growth", 0.25);
        double spacing = Math.max(0.3, number("ring-spacing", 0.8));
        double twist = number("twist", 0.45);
        double speed = number("particle-speed", 0.08);
        double spin = age * number("spin-speed", 0.35);

        for (double h = 0; h < height; h += spacing) {
            double radius = base + h * growth;
            for (int k = 0; k < arms.size(); k++) {
                double angle = spin + h * twist + k * (Math.PI * 2 / arms.size());
                Location point = center.clone().add(Math.cos(angle) * radius, h, Math.sin(angle) * radius);
                // Con cantidad 0 el desplazamiento es la dirección: la partícula sale girando.
                ParticleLook arm = arms.get(k);
                world.spawnParticle(arm.particle(), point, 0, -Math.sin(angle), 0.2, Math.cos(angle), speed, arm.data(),
                        true);
            }
        }

        int dust = (int) number("ground-particles", 24);
        Block floor = center.clone().subtract(0, 1, 0).getBlock();
        if (dust > 0 && floor.getType().isSolid()) {
            double spread = base + growth * 10;
            world.spawnParticle(Particle.BLOCK, center, dust, spread, 0.4, spread, 0, floor.getBlockData(), true);
        }
    }

    private void pull(double height) {

        double reach = number("radius", 12);
        double core = number("core-radius", 2.5);
        double pull = strength("pull", 0.45);
        double swirl = number("swirl", 0.8);
        double lift = number("lift-speed", 0.55);
        double liftHeight = number("lift-height", 14);
        double fling = strength("fling", 1.5);
        double flingUp = number("fling-up", 0.6);
        int cooldown = (int) number("fling-cooldown", 60);
        double damage = strength("damage", 4);

        for (Entity entity : world.getNearbyEntities(center.clone().add(0, height / 2, 0), reach, height / 2 + 2, reach)) {

            if (!hits(entity) || flungUntil.getOrDefault(entity.getUniqueId(), -1) > age) {
                continue;
            }

            Vector toCenter = center.toVector().subtract(entity.getLocation().toVector()).setY(0);
            double distance = toCenter.length();
            if (distance > reach) {
                continue;
            }
            if (distance < 0.01) {
                toCenter = new Vector(1, 0, 0);
                distance = 1;
            }

            Vector inward = toCenter.clone().multiply(1 / distance);
            Vector around = new Vector(-inward.getZ(), 0, inward.getX());

            if (distance > core) {
                Vector velocity = entity.getVelocity().multiply(0.5)
                        .add(inward.multiply(pull * (1 - distance / reach) + 0.05))
                        .add(around.multiply(pull * swirl));
                entity.setVelocity(velocity.setY(Math.max(velocity.getY(), 0.08)));
            } else if (entity.getLocation().getY() - center.getY() < liftHeight) {
                entity.setVelocity(around.multiply(0.3).setY(lift));
            } else {
                entity.setVelocity(around.multiply(fling).setY(flingUp));
                flungUntil.put(entity.getUniqueId(), age + cooldown);
                if (entity instanceof LivingEntity living) {
                    strike(living, damage);
                }
            }
        }
    }

    /** Escombros girando por el embudo; los que caducan (30 s) se reponen. */
    private void spinDebris(double height) {

        debris.removeIf(block -> !block.isValid());

        int max = (int) number("debris", 14);
        if (debris.size() < max && age % 10 < period) {
            BlockData data = debrisData();
            if (data != null) {
                FallingBlock block = engine.fallingBlock(center.clone().add(0, 1 + random() * height * 0.6, 0), data, null);
                block.setGravity(false);
                debris.add(block);
            }
        }

        double base = number("funnel-base-radius", 0.6) + 1;
        double growth = number("funnel-growth", 0.25);

        for (FallingBlock block : debris) {
            Vector offset = block.getLocation().toVector().subtract(center.toVector());
            double h = Math.clamp(offset.getY() + 0.3, 1, height * 0.8);
            double angle = Math.atan2(offset.getZ(), offset.getX()) + 0.5;
            double radius = base + h * growth;
            Vector target = center.toVector().add(new Vector(Math.cos(angle) * radius, h, Math.sin(angle) * radius));
            block.setVelocity(target.subtract(block.getLocation().toVector()).multiply(1.0 / period));
        }
    }

    /**
     * Arranca hasta {@code break-per-tick} bloques de {@code breakable-blocks} en la cima de columnas al
     * azar dentro de {@code break-radius}: primero la hierba y las copas, luego lo de debajo. Respeta
     * las zonas protegidas y las hojas puestas por jugadores, y lo arrancado sale girando por el embudo.
     */
    private void rip(double height) {

        if (!flag("break-blocks", false)) {
            return;
        }

        Set<Material> breakable = materials("breakable-blocks", BREAKABLE);
        double radius = number("break-radius", 5);
        double top = center.getY() + Math.min(height, number("break-height", 16));
        List<Block> candidates = new ArrayList<>();

        for (int i = 0; i < (int) number("break-per-tick", 6); i++) {
            double angle = random() * Math.PI * 2;
            double distance = Math.sqrt(random()) * radius;
            Block block = world.getHighestBlockAt(center.getBlockX() + (int) Math.round(Math.cos(angle) * distance),
                    center.getBlockZ() + (int) Math.round(Math.sin(angle) * distance), HeightMap.WORLD_SURFACE);
            if (block.getY() < center.getY() - 3 || block.getY() > top || !breakable.contains(block.getType())
                    || block.getBlockData() instanceof Leaves leaves && leaves.isPersistent()
                    || candidates.contains(block)) {
                continue;
            }
            candidates.add(block);
            // Las plantas altas son dos bloques: se arrancan juntos para no dejar medio girasol flotando.
            if (block.getBlockData() instanceof Bisected half) {
                Block other = block.getRelative(half.getHalf() == Bisected.Half.TOP ? BlockFace.DOWN : BlockFace.UP);
                if (other.getType() == block.getType() && !candidates.contains(other)) {
                    candidates.add(other);
                }
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        int restore = (int) (number("restore-seconds", 120) * 20);
        int maxDebris = (int) number("debris", 14) * 2;
        for (Block block : engine.breakable(center.getBlock(), candidates)) {
            BlockData data = block.getBlockData();
            engine.breakTemporarily(block, restore);
            world.spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 6, 0.3, 0.3, 0.3, 0, data, true);
            if (debris.size() < maxDebris && !(data instanceof Bisected) && random() < 0.5) {
                FallingBlock flying = engine.fallingBlock(block.getLocation().add(0.5, 0.5, 0.5), data, null);
                flying.setGravity(false);
                debris.add(flying);
            }
        }
    }

    /** Uno de {@code debris-blocks}, o a veces el bloque que pisa si {@code debris-from-ground}. */
    private BlockData debrisData() {
        Block floor = center.clone().subtract(0, 1, 0).getBlock();
        if (flag("debris-from-ground", true) && DisasterEngine.canFake(floor) && random() < 0.5) {
            return floor.getBlockData();
        }
        return pick(blocks("debris-blocks", List.of("DIRT", "OAK_LEAVES", "COBBLESTONE")));
    }

    @Override
    void cleanup() {
        debris.forEach(engine::discard);
        debris.clear();
        super.cleanup();
    }

}
