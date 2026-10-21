package com.sack.rpgroll.disasters.engine;

import com.sack.rpgroll.disasters.core.DisasterPhenomenon;
import com.sack.rpgroll.disasters.engine.Looks.ParticleLook;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Una ola gigante que nace en el agua (o donde sea, si no hay) y avanza en línea recta hacia un
 * jugador. Es un muro de agua de verdad que va pasando por el terreno: arrastra y daña a lo que pilla,
 * le quita el aire, arranca plantas y lo que diga {@code breakable-blocks}, y al pasar deja charcos
 * que se secan solos. Los edificios la paran: pasa por los huecos.
 * <p>
 * El agua de la ola se quita sola al pasar, y además se apunta en {@link WorldDamage} para que una
 * caída del servidor a mitad de ola no la deje en el mundo. Lo que arranca se repone como todo lo demás.
 * <p>
 * Parámetros y valores por defecto en {@code disasters/_reference.yml}.
 */
final class Tsunami extends Phenomenon {

    /** Agua de la ola: si el servidor se cae antes de quitarla, la regeneración lo hace a los tantos segundos. */
    private static final double SAFETY_SECONDS = 30;

    /** Una casilla de agua de la ola y el paso del frente en el que se puso. */
    private record Cell(Block block, int step) {
    }

    private final Deque<Cell> wall = new ArrayDeque<>();
    private final Map<UUID, Integer> lastHit = new HashMap<>();
    private Location origin;
    private Vector direction;
    private double travelled;
    /** Hasta dónde llega esta ola: el jugador al que va más {@code overshoot}. */
    private double limit;
    private int step = -1;
    private int nextWave;

    Tsunami(DisasterEngine engine, World world, DisasterPhenomenon params, Power power, int durationTicks) {
        super(engine, world, params, power, durationTicks, 2);
    }

    @Override
    boolean defaultStorm() {
        return true;
    }

    @Override
    void tick() {

        if (origin == null) {
            if (age >= nextWave) {
                nextWave = age + (int) Math.max(period, number("interval", 400));
                launch();
            }
            return;
        }

        travelled += number("speed", 0.25) * period;
        while (step < (int) travelled) {
            step++;
            advance(step);
        }
        recede(false);
        sweepEntities();

        if (travelled >= limit) {
            recede(true);
            origin = null;
        }
    }

    // ----- La ola -----

    /** Elige a quién va y desde dónde: en el agua más cercana a {@code spawn-distance}, si la hay. */
    private void launch() {

        double chance = Math.min(1, strength("chance", 1));
        List<Player> targets = new ArrayList<>(engine.targets(world));
        java.util.Collections.shuffle(targets);
        for (Player player : targets) {
            if (random() >= chance) {
                continue;
            }
            Location target = player.getLocation();
            Location start = startFor(target);
            if (start == null) {
                continue;
            }
            origin = start;
            direction = target.toVector().subtract(start.toVector()).setY(0).normalize();
            limit = Math.hypot(target.getX() - start.getX(), target.getZ() - start.getZ()) + scaled("overshoot", 24);
            travelled = 0;
            step = -1;
            lastHit.clear();
            sound("start-sound", "AMBIENT_UNDERWATER_ENTER 4 0.5").play(world, target);
            return;
        }
    }

    /** Un punto con agua a {@code spawn-distance} (o a 1,5 y 2 veces eso, el más cercano que haya). */
    private Location startFor(Location target) {

        double distance = number("spawn-distance", 48);
        for (double factor : new double[] {1, 1.5, 2}) {
            List<Location> water = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2 * i / 24;
                Location at = target.clone().add(Math.cos(angle) * distance * factor, 0,
                        Math.sin(angle) * distance * factor);
                if (world.getHighestBlockAt(at, HeightMap.MOTION_BLOCKING).getType() == Material.WATER) {
                    water.add(at);
                }
            }
            if (!water.isEmpty()) {
                return pick(water);
            }
        }
        if (flag("require-water", false)) {
            return null;
        }
        double angle = random() * Math.PI * 2;
        return target.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
    }

    /** El frente da un paso: pone su muro de agua sobre el terreno y arranca lo blando que pilla. */
    private void advance(int at) {

        WorldDamage damage = engine.damage();
        double width = scaled("width", 28);
        double height = scaled("height", 7);
        Vector across = new Vector(-direction.getZ(), 0, direction.getX());
        Location front = origin.clone().add(direction.clone().multiply(at));
        BlockData water = waterLevel(1);
        Set<Material> breakable = materials("breakable-blocks", List.of("*_LEAVES"));
        boolean breakSoft = flag("break-blocks", true);
        Set<Block> seen = new HashSet<>();
        List<ParticleLook> crest = particles("crest-particles", List.of("SPLASH*6", "CLOUD*1", "BUBBLE_POP*2"));

        // Paso de 0.7: en diagonal, de 1 en 1 quedarían agujeros en el muro.
        for (double side = -width / 2; side <= width / 2; side += 0.7) {
            Location column = front.clone().add(across.clone().multiply(side));
            // Más alta en el centro y baja hacia las puntas.
            double edge = Math.abs(side) / (width / 2);
            int columnHeight = (int) Math.max(1, Math.round(height * (1 - edge * edge * edge * edge)));
            // El suelo, o la superficie del mar (el agua cuenta en este mapa de alturas; las hojas no).
            Block surface = world.getHighestBlockAt(column, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Block first = surface.getRelative(0, 1, 0);
            if (!seen.add(first)) {
                continue;
            }

            boolean placeable = damage.enabled() && damage.canPlace(first);
            for (int y = 0; y < columnHeight; y++) {
                Block block = first.getRelative(0, y, 0);
                Material type = block.getType();
                if (!type.isAir() && !block.isLiquid()) {
                    boolean soft = breakSoft && !type.isSolid();
                    if ((soft || breakable.contains(type)) && placeable
                            && !damage.filter(block, List.of(block)).isEmpty()) {
                        damage.remove(block, regenerate());
                    } else {
                        continue;
                    }
                }
                if (placeable && block.getType().isAir()) {
                    damage.change(block, water, SAFETY_SECONDS);
                    wall.add(new Cell(block, at));
                } else if (!placeable && y % 2 == 0) {
                    // Sin poder poner agua (zona segura, claim, destrucción apagada) la ola se ve igual.
                    world.spawnParticle(org.bukkit.Particle.FALLING_WATER, block.getLocation().add(0.5, 0.5, 0.5), 4,
                            0.4, 0.4, 0.4, 0, null, true);
                }
            }

            if (random() < 0.35) {
                Location top = first.getLocation().add(0.5, columnHeight, 0.5);
                for (ParticleLook look : crest) {
                    look.spawn(world, top, 6, 0.6, 0.3, 0.6, 0.05);
                }
            }
        }

        if (at % 10 == 0) {
            sound("sound", "WEATHER_RAIN_ABOVE 4 0.5").play(world, front);
            sound("roar-sound", "ENTITY_PLAYER_SPLASH_HIGH_SPEED 4 0.5").play(world, front);
        }
    }

    /**
     * Quita el agua que ya quedó atrás ({@code thickness} pasos por detrás del frente), o toda con
     * {@code all}. Parte se queda como charco ({@code flood-chance}) hasta {@code flood-seconds}.
     */
    private void recede(boolean all) {

        int thickness = (int) Math.max(1, number("thickness", 2));
        double floodChance = number("flood-chance", 0.3);
        double floodSeconds = number("flood-seconds", 45);
        WorldDamage damage = engine.damage();

        while (!wall.isEmpty() && (all || wall.peek().step() <= step - thickness)) {
            Cell cell = wall.poll();
            Block block = cell.block();
            if (block.getType() != Material.WATER) {
                continue;
            }
            // Solo el agua que queda tocando el suelo puede quedarse de charco.
            boolean onGround = block.getRelative(0, -1, 0).getType().isSolid();
            if (onGround && floodSeconds > 0 && random() < floodChance) {
                damage.change(block, waterLevel(6), floodSeconds);
            } else {
                block.setBlockData(Material.AIR.createBlockData(), false);
            }
        }
    }

    /** Agua que no es fuente: si algo la actualiza, se seca en vez de extenderse. */
    private static BlockData waterLevel(int level) {
        Levelled water = (Levelled) Material.WATER.createBlockData();
        water.setLevel(level);
        return water;
    }

    /** Lo que está en el frente sale arrastrado en la dirección de la ola, recibe daño y pierde aire. */
    private void sweepEntities() {

        if (step < 0) {
            return;
        }
        double width = scaled("width", 28);
        double height = scaled("height", 7);
        Location front = origin.clone().add(direction.clone().multiply(travelled));
        double reach = width / 2 + 2;
        double push = strength("push", 0.9);
        double damageAmount = strength("damage", 3);
        int air = (int) Math.round(strength("drown", 60));
        int cooldown = (int) number("hit-cooldown", 20);

        for (Entity entity : world.getNearbyEntities(front, reach, height + 4, reach)) {
            Vector offset = entity.getLocation().toVector().subtract(front.toVector());
            double along = offset.dot(direction);
            double sideways = Math.abs(offset.getX() * -direction.getZ() + offset.getZ() * direction.getX());
            if (along < -3 || along > 1.5 || sideways > width / 2 || !hits(entity)) {
                continue;
            }

            entity.setVelocity(direction.clone().multiply(push).setY(Math.max(0.2, number("lift", 0.3))));
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            Integer last = lastHit.get(entity.getUniqueId());
            if (last == null || age - last >= cooldown) {
                lastHit.put(entity.getUniqueId(), age);
                strike(living, damageAmount);
                if (air > 0) {
                    living.setRemainingAir(Math.max(-20, living.getRemainingAir() - air));
                }
            }
        }
    }

    @Override
    void cleanup() {
        recede(true);
        super.cleanup();
    }

}
