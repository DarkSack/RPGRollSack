package com.sack.rpgroll.seasons.disaster;

import com.sack.rpgroll.seasons.core.DisasterPhenomenon;
import com.sack.rpgroll.seasons.disaster.Looks.SoundLook;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * El suelo tiembla bajo cada jugador: sacudidas, polvo del bloque que pisa y estruendo. Se abren
 * grietas en el suelo (solo visuales) y, bajo techo o en cuevas, caen rocas que dañan al dar.
 * Es el único fenómeno que también ocurre en el Nether y el End.
 * <p>
 * Parámetros y valores por defecto en {@code disasters/_reference.yml}.
 */
final class Earthquake extends Phenomenon {

    Earthquake(DisasterEngine engine, World world, DisasterPhenomenon params, double intensity, int durationTicks) {
        super(engine, world, params, intensity, durationTicks, 10);
    }

    @Override
    void tick() {

        double shake = strength("shake", 0.18);
        double shakeUp = number("shake-up", 0.6);
        int dust = (int) number("ground-particles", 12);
        SoundLook rumble = sound("rumble-sound", "BLOCK_BASALT_BREAK 1 0.5");
        int boomEvery = (int) number("boom-every", 40);
        boolean boom = boomEvery > 0 && age % boomEvery < period;

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

            if (random() < number("cracks", 0.25)) {
                crack(at);
            }
            if (random() < strength("rocks", 0.12)) {
                dropRock(player);
            }
        }
    }

    /** Una grieta de {@code crack-blocks} en el suelo, cerca del jugador, que solo ven los clientes. */
    private void crack(Location near) {

        List<BlockData> looks = blocks("crack-blocks", List.of("BLACKSTONE"));
        int ticks = (int) (number("crack-seconds", 30) * 20);
        if (looks.isEmpty() || ticks <= 0) {
            return;
        }

        double angle = random() * Math.PI * 2;
        Vector direction = new Vector(Math.cos(angle), 0, Math.sin(angle));
        double offset = number("crack-distance", 2) + random() * 3;
        Location start = near.clone().add(direction.clone().multiply(offset));
        double min = number("crack-length-min", 5);
        double max = Math.max(min, number("crack-length-max", 11));
        int length = (int) Math.min(number("crack-length-cap", 16), (min + random() * (max - min)) * intensity);
        double jitter = number("crack-jitter", 1);

        Map<Block, BlockData> blocks = new HashMap<>();

        for (int i = 0; i < length; i++) {
            Location step = start.clone().add(direction.clone().multiply(i))
                    .add(-direction.getZ() * (random() - 0.5) * jitter, 0, direction.getX() * (random() - 0.5) * jitter);
            Block floor = floorAt(step);
            if (floor != null && DisasterEngine.canFake(floor)) {
                blocks.put(floor, pick(looks));
            }
        }

        engine.fake(blocks, ticks);
    }

    /** El suelo a la altura del jugador: el primer bloque macizo de un poco por encima a un poco por debajo. */
    private Block floorAt(Location location) {
        Block block = location.getBlock().getRelative(BlockFace.UP);
        for (int i = 0; i < 5; i++, block = block.getRelative(BlockFace.DOWN)) {
            if (block.getType().isSolid() && !block.getRelative(BlockFace.UP).getType().isSolid()) {
                return block;
            }
        }
        return null;
    }

    /**
     * Bajo techo, una roca cae del techo cerca del jugador; a cielo abierto no hay de dónde. La roca
     * es uno de {@code rock-blocks}, o el bloque del propio techo si la lista está vacía.
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

        BlockData fromList = pick(blocks("rock-blocks", List.of()));
        BlockData rock = fromList != null ? fromList
                : DisasterEngine.canFake(ceiling) ? ceiling.getBlockData() : block("rock-fallback", "COBBLESTONE");
        double scatter = number("rock-scatter", 3);
        Location from = ceiling.getLocation().add(0.5 + (random() - 0.5) * scatter, -1, 0.5 + (random() - 0.5) * scatter);
        if (from.getBlock().getType().isSolid()) {
            return;
        }

        SoundLook crash = sound("rock-sound", "BLOCK_STONE_BREAK 1 0.7");
        double radius = number("rock-radius", 1.2);
        crash.play(world, from);

        engine.fallingBlock(from, rock, landed -> {
            world.spawnParticle(Particle.BLOCK, landed, 20, 0.4, 0.2, 0.4, 0, rock);
            crash.play(world, landed);
            for (Entity entity : world.getNearbyEntities(landed, radius, 1.5, radius)) {
                if (entity instanceof LivingEntity living && hits(entity)) {
                    strike(living, strength("damage", 3));
                }
            }
        });
    }

}
