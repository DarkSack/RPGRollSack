package com.sack.rpgroll.seasons.disaster;

import com.sack.rpgroll.seasons.core.DisasterPhenomenon;
import com.sack.rpgroll.seasons.disaster.Looks.ParticleLook;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bloques que caen en diagonal con estela y estallan cerca de los jugadores: daño y empuje a quien
 * esté a cielo abierto en el radio, sin romper bloques, y un cráter que solo se ve un rato. Con
 * otros bloques y partículas sirve para granizo, lluvia de hielo o de escombros.
 * <p>
 * Parámetros y valores por defecto en {@code disasters/_reference.yml}.
 */
final class MeteorStorm extends Phenomenon {

    private final Map<FallingBlock, Vector> meteors = new HashMap<>();
    private int nextWave;

    MeteorStorm(DisasterEngine engine, World world, DisasterPhenomenon params, double intensity, int durationTicks) {
        super(engine, world, params, intensity, durationTicks, 1);
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

        FallingBlock meteor = engine.fallingBlock(start, data, this::impact);
        meteor.setGravity(false);
        meteors.put(meteor, direction.multiply(number("speed", 1.4)));
    }

    private void impact(Location at) {

        for (ParticleLook look : particles("impact-particles", List.of("EXPLOSION_EMITTER*1", "LAVA*25"))) {
            look.spawn(world, at, 10, 1.5, 0.5, 1.5, 0);
        }
        sound("impact-sound", "ENTITY_GENERIC_EXPLODE 4 0.7").play(world, at);

        double radius = number("impact-radius", 4);
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

    /** Un cráter que solo ven los clientes: {@code crater-center} en medio y {@code crater-blocks} alrededor. */
    private void crater(Location at) {

        int radius = (int) number("crater-radius", 2);
        int ticks = (int) (number("crater-seconds", 40) * 20);
        List<BlockData> ring = blocks("crater-blocks", List.of("BLACKSTONE", "COBBLED_DEEPSLATE"));
        if (radius <= 0 || ticks <= 0 || ring.isEmpty()) {
            return;
        }

        BlockData middle = block("crater-center", "MAGMA_BLOCK");
        Map<Block, BlockData> blocks = new HashMap<>();
        double limit = radius * radius + 1;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > limit) {
                    continue;
                }
                Block top = world.getHighestBlockAt(at.getBlockX() + x, at.getBlockZ() + z, HeightMap.MOTION_BLOCKING);
                if (DisasterEngine.canFake(top)) {
                    blocks.put(top, x == 0 && z == 0 ? middle : pick(ring));
                }
            }
        }

        engine.fake(blocks, ticks);
    }

    @Override
    void cleanup() {
        meteors.keySet().forEach(engine::discard);
        meteors.clear();
        super.cleanup();
    }

}
