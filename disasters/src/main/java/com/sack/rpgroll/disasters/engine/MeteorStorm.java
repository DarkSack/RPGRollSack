package com.sack.rpgroll.disasters.engine;

import com.sack.rpgroll.disasters.core.DisasterPhenomenon;
import com.sack.rpgroll.disasters.engine.Looks.ParticleLook;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Meteoritos que caen en diagonal con estela y estallan cerca de los jugadores: daño y empuje a quien
 * esté en el radio y un cráter de verdad, con el borde chamuscado y un núcleo de {@code core-blocks}
 * (magma y, a veces, menas que merece la pena ir a picar). El cráter se repone con el tiempo (ver
 * {@link WorldDamage}); lo que alguien haya picado del núcleo se rellena igual. Con otros bloques,
 * partículas y sin cráter sirve para granizo o lluvia de escombros.
 * <p>
 * Parámetros y valores por defecto en {@code disasters/_reference.yml}.
 */
final class MeteorStorm extends Phenomenon {

    private final Map<FallingBlock, Vector> meteors = new HashMap<>();
    private int nextWave;

    MeteorStorm(DisasterEngine engine, World world, DisasterPhenomenon params, Power power, int durationTicks) {
        super(engine, world, params, power, durationTicks, 1);
    }

    @Override
    void tick() {

        if (age >= nextWave) {
            nextWave = age + (int) Math.max(1, number("interval", 30));
            double chance = Math.min(1, strength("chance", 0.5));
            for (Player player : engine.targets(world)) {
                if (random() < chance) {
                    launch(player.getLocation());
                }
            }
        }

        meteors.entrySet().removeIf(entry -> !entry.getKey().isValid());

        int maxFlight = (int) number("max-flight-ticks", 120);
        List<ParticleLook> trail = particles("trail-particles", List.of("FLAME*4", "LARGE_SMOKE*2"));

        for (var entry : Map.copyOf(meteors).entrySet()) {
            FallingBlock meteor = entry.getKey();
            if (meteor.getTicksLived() > maxFlight) {
                engine.discard(meteor);
                continue;
            }
            meteor.setVelocity(entry.getValue());
            for (ParticleLook look : trail) {
                look.spawn(world, meteor.getLocation(), 3, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    private void launch(Location near) {

        BlockData data = pick(blocks("blocks", List.of("MAGMA_BLOCK")));
        if (data == null) {
            return;
        }

        double angle = random() * Math.PI * 2;
        double min = number("spread-min", 0);
        double distance = min + random() * Math.max(0, number("spread", 32) - min);
        Location impact = near.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
        impact.setY(world.getHighestBlockYAt(impact.getBlockX(), impact.getBlockZ(), HeightMap.MOTION_BLOCKING) + 1);

        if (engine.inSafeZone(impact)) {
            return;
        }

        double slant = number("slant", 1);
        Vector direction = new Vector((random() - 0.5) * slant, -1.6, (random() - 0.5) * slant).normalize();
        Location start = impact.clone().subtract(direction.clone().multiply(number("start-distance", 48)));
        if (start.getY() > world.getMaxHeight() - 2) {
            start.setY(world.getMaxHeight() - 2);
            direction = impact.toVector().subtract(start.toVector()).normalize();
        }

        Vector velocity = direction.multiply(number("speed", 1.4));
        FallingBlock meteor = engine.fallingBlock(start, data, this::impact);
        meteor.setGravity(false);
        meteors.put(meteor, velocity);

        int extra = (int) Math.round(scaled("meteor-size", 1)) - 1;
        for (int i = 0; i < extra; i++) {
            Vector offset = new Vector(random() - 0.5, random() - 0.5, random() - 0.5).normalize().multiply(0.9);
            FallingBlock chunk = engine.fallingBlock(start.clone().add(offset), pick(blocks("blocks",
                    List.of("MAGMA_BLOCK"))), null);
            chunk.setGravity(false);
            meteors.put(chunk, velocity);
        }
    }

    private void impact(Location at) {

        for (ParticleLook look : particles("impact-particles", List.of("EXPLOSION_EMITTER*1", "LAVA*25"))) {
            look.spawn(world, at, 10, 1.5, 0.5, 1.5, 0);
        }
        sound("impact-sound", "ENTITY_GENERIC_EXPLODE 4 0.7").play(world, at);

        double radius = scaled("impact-radius", 4);
        double damage = strength("damage", 8);
        double knockback = number("knockback", 0.8);
        boolean onlyOpenSky = flag("only-open-sky", true);

        for (Entity entity : world.getNearbyEntities(at, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || !hits(entity)
                    || (onlyOpenSky && !DisasterEngine.underOpenSky(entity.getLocation()))) {
                continue;
            }
            double distance = entity.getLocation().distance(at);
            if (distance > radius) {
                continue;
            }
            strike(living, damage * (1 - distance / (radius + 1)));
            Vector away = entity.getLocation().toVector().subtract(at.toVector()).setY(0);
            living.setVelocity((away.lengthSquared() < 0.01 ? new Vector() : away.normalize().multiply(knockback))
                    .setY(knockback * 0.6));
        }

        crater(at);
    }

    /**
     * Un cráter de verdad: una semiesfera de {@code crater-radius} vaciada bajo el impacto, con el
     * suelo de alrededor chamuscado ({@code crater-blocks}, con probabilidad {@code crater-scorch}) y
     * {@code core-size} bloques de {@code core-blocks} en el fondo. 0 de radio = sin cráter.
     */
    private void crater(Location at) {

        WorldDamage damage = engine.damage();
        double radius = scaled("crater-radius", 2.5);
        if (radius < 1 || !damage.enabled()) {
            return;
        }

        Block center = at.getBlock();
        int reach = (int) Math.ceil(radius);
        List<Block> hollow = new ArrayList<>();
        for (int x = -reach; x <= reach; x++) {
            for (int y = -reach; y <= 1; y++) {
                for (int z = -reach; z <= reach; z++) {
                    if (x * x + y * y * 1.6 + z * z <= radius * radius) {
                        hollow.add(center.getRelative(x, y, z));
                    }
                }
            }
        }

        List<Block> removed = damage.filter(center, hollow);
        removed.forEach(block -> damage.remove(block, regenerate()));

        List<BlockData> scorch = blocks("crater-blocks", List.of("BLACKSTONE", "COBBLED_DEEPSLATE", "MAGMA_BLOCK"));
        double scorchChance = number("crater-scorch", 0.6);
        List<Block> floor = new ArrayList<>();
        for (Block block : removed) {
            Block below = block.getRelative(BlockFace.DOWN);
            if (below.getType().isSolid() && !removed.contains(below)) {
                floor.add(below);
            }
        }
        if (!scorch.isEmpty()) {
            for (Block block : damage.filter(center, floor)) {
                if (random() < scorchChance) {
                    damage.change(block, pick(scorch), regenerate());
                }
            }
        }

        List<BlockData> core = blocks("core-blocks", List.of("MAGMA_BLOCK*6", "DEEPSLATE_COPPER_ORE*3",
                "DEEPSLATE_IRON_ORE*3", "DEEPSLATE_GOLD_ORE*2", "DEEPSLATE_DIAMOND_ORE"));
        int coreSize = (int) Math.round(scaled("core-size", 3));
        if (core.isEmpty() || coreSize <= 0 || floor.isEmpty()) {
            return;
        }
        floor.sort(java.util.Comparator.comparingDouble(block -> block.getLocation().distanceSquared(center.getLocation())));
        for (Block block : floor.subList(0, Math.min(coreSize, floor.size()))) {
            damage.change(block, pick(core), regenerate());
        }
    }

    @Override
    void cleanup() {
        meteors.keySet().forEach(engine::discard);
        meteors.clear();
        super.cleanup();
    }

}
