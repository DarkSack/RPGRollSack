package com.sack.rpgroll.disasters.engine;

import com.sack.rpgroll.disasters.core.DisasterPhenomenon;
import com.sack.rpgroll.disasters.engine.Looks.ParticleLook;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Una ola gigante que nace en el agua (o donde sea, si no hay) y avanza en línea recta hacia un
 * jugador. Tiene forma de ola de verdad: frente cóncavo, cresta que se curva hacia delante y punta que
 * cae, con el hueco del tubo debajo, y un lomo de agua detrás. Se dibuja con bloques de agua que se
 * ponen y se quitan a cada paso. El frente arrasa lo que diga {@code breakable-blocks} (y lo blando) y
 * lanza parte como escombros; contra lo que no puede romper se estrella. Al pasar deja charcos que se
 * secan solos.
 * <p>
 * El agua de la ola se quita sola al pasar, y además se apunta en {@link WorldDamage} para que una
 * caída del servidor a mitad de ola no la deje en el mundo. Lo que arranca se repone como todo lo demás.
 * <p>
 * Parámetros y valores por defecto en {@code disasters/_reference.yml}.
 */
final class Tsunami extends Phenomenon {

    /** Agua de la ola: si el servidor se cae antes de quitarla, la regeneración lo hace a los tantos segundos. */
    private static final double SAFETY_SECONDS = 30;
    /** Separación entre columnas y cortes: con 1 quedarían agujeros en diagonal. */
    private static final double STEP = 0.7;

    /**
     * Un corte de la ola a {@code forward} bloques del pie del frente (positivo delante, negativo
     * detrás), con agua de {@code bottom} a {@code top} sobre la base. {@code tip}: la punta que cae.
     * Detrás ({@code forward < 0}) el agua llega hasta el suelo, como mucho {@code bottom} por debajo.
     */
    record Slice(double forward, int bottom, int top, boolean tip) {
    }

    private Set<Block> water = new HashSet<>();
    private final List<FallingBlock> debris = new ArrayList<>();
    private final Map<UUID, Integer> lastHit = new HashMap<>();
    private Location origin;
    private Vector direction;
    private double travelled;
    /** Hasta dónde llega esta ola: el jugador al que va más {@code overshoot}. */
    private double limit;
    private int step = -1;
    private int nextWave;
    /** Altura del pie de la ola en cada columna: sube por las cuestas y no trepa paredes. */
    private int[] bases;
    /** Pasos seguidos que cada columna lleva frente a algo más alto que no ha subido. */
    private int[] blocked;

    Tsunami(DisasterEngine engine, World world, DisasterPhenomenon params, Power power, int durationTicks) {
        super(engine, world, params, power, durationTicks, 2);
    }

    @Override
    boolean defaultStorm() {
        return true;
    }

    @Override
    void tick() {

        debris.removeIf(block -> !block.isValid());
        if (origin == null) {
            if (age >= nextWave) {
                nextWave = age + (int) Math.max(period, number("interval", 400));
                launch();
            }
            return;
        }

        travelled += number("speed", 0.25) * period;
        if (step < (int) travelled) {
            step = (int) travelled;
            render();
        }
        sweepEntities();

        if (travelled >= limit) {
            leave(water);
            water = new HashSet<>();
            origin = null;
        }
    }

    // ----- La forma -----

    /**
     * El perfil de una columna de {@code height} bloques. Delante del pie, a cada altura el frente
     * llega a {@code reach · (y / alto)²} (reach = alto · curl): cóncavo abajo y volcado arriba. La
     * punta cuelga casi la mitad del alto; entre ella y el frente queda el hueco del tubo. Detrás, el
     * lomo baja hasta {@code back · alto}.
     */
    static List<Slice> profile(int height, double curl, double back) {

        List<Slice> slices = new ArrayList<>();
        int top = Math.max(0, height - 1);
        double reach = Math.max(0, Math.round(height * curl));
        int drop = (int) Math.round(height * 0.45);

        List<Double> ahead = new ArrayList<>();
        for (double k = 0; k < reach; k += STEP) {
            ahead.add(k);
        }
        ahead.add(reach);
        for (double k : ahead) {
            // El agua de este corte empieza a la altura donde el frente ya llega hasta aquí.
            int from = 0;
            while (from <= top && (top == 0 ? 0 : reach * Math.pow((double) from / top, 2)) < k - 1e-9) {
                from++;
            }
            boolean tip = reach > 0 && k >= reach - STEP;
            int bottom = tip ? Math.min(from, Math.max(0, top - drop)) : from;
            if (bottom <= top) {
                slices.add(new Slice(k, bottom, top, tip));
            }
            // La cresta redondeada: una capa más por encima en el tramo central.
            if (reach >= 2 && k >= reach * 0.2 && k <= reach * 0.7) {
                slices.add(new Slice(k, top + 1, top + 1, false));
            }
        }

        double length = Math.max(1, height * back);
        for (double a = STEP; a <= length; a += STEP) {
            int h = (int) Math.round(height * Math.pow(1 - a / length, 1.3));
            if (h > 0) {
                slices.add(new Slice(-a, -3, h - 1, false));
            }
        }
        return slices;
    }

    // ----- La ola -----

    /** Elige a quién va y desde dónde: en el agua más cercana a {@code spawn-distance}, si la hay. */
    private void launch() {

        double chance = Math.min(1, strength("chance", 1));
        List<Player> targets = new ArrayList<>(engine.targets(world));
        Collections.shuffle(targets);
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
            bases = null;
            lastHit.clear();
            sound("start-sound", "AMBIENT_UNDERWATER_ENTER 4 0.5").play(world, target);
            return;
        }
    }

    /** Un punto con agua a {@code spawn-distance} (o a 1,5 y 2 veces eso, el más cercano que haya). */
    private Location startFor(Location target) {

        double distance = number("spawn-distance", 56);
        for (double factor : new double[] {1, 1.5, 2}) {
            List<Location> found = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2 * i / 24;
                Location at = target.clone().add(Math.cos(angle) * distance * factor, 0,
                        Math.sin(angle) * distance * factor);
                if (world.getHighestBlockAt(at, HeightMap.MOTION_BLOCKING).getType() == Material.WATER) {
                    found.add(at);
                }
            }
            if (!found.isEmpty()) {
                return pick(found);
            }
        }
        if (flag("require-water", false)) {
            return null;
        }
        double angle = random() * Math.PI * 2;
        return target.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
    }

    /**
     * La superficie en ese punto: el suelo o el mar (las hojas no cuentan). El agua de la propia ola y
     * el aire de su tubo no son suelo: sin esto la ola se subiría encima de sí misma a cada paso.
     */
    private int ground(Location at) {
        int y = world.getHighestBlockYAt(at, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        Block block = world.getBlockAt(at.getBlockX(), y, at.getBlockZ());
        while (y > world.getMinHeight() && (water.contains(block) || block.getType().isAir())) {
            block = block.getRelative(0, -1, 0);
            y--;
        }
        return y;
    }

    /** Dibuja la ola en su sitio nuevo: pone el agua que falta, quita la que sobra y arrasa el frente. */
    private void render() {

        WorldDamage damage = engine.damage();
        double width = scaled("width", 40);
        double height = scaled("height", 12);
        double curl = number("curl", 0.65);
        double back = number("back-length", 1.0);
        int climb = (int) number("climb", 2);
        Vector across = new Vector(-direction.getZ(), 0, direction.getX());
        Location front = origin.clone().add(direction.clone().multiply(step));
        Set<Material> breakable = materials("breakable-blocks", List.of("*_LEAVES", "*_LOG", "*_WOOD", "*_PLANKS",
                "*_FENCE", "*_FENCE_GATE", "*_DOOR", "*_TRAPDOOR", "*GLASS*", "*_WOOL", "*_CARPET", "*_SLAB", "*_STAIRS",
                "HAY_BLOCK", "MELON", "PUMPKIN", "BAMBOO", "SUGAR_CANE", "CACTUS", "FARMLAND", "DIRT_PATH"));
        boolean breakSoft = flag("break-blocks", true);
        double debrisChance = number("debris-chance", 0.35);
        int maxDebris = (int) scaled("debris", 30);
        BlockData body = waterLevel(1);
        BlockData falling = waterLevel(8);

        int columns = (int) Math.floor(width / STEP) + 1;
        if (bases == null || bases.length != columns) {
            bases = new int[columns];
            blocked = new int[columns];
            for (int c = 0; c < columns; c++) {
                bases[c] = ground(front.clone().add(across.clone().multiply(-width / 2 + c * STEP))) + 1;
            }
        }

        Set<Block> next = new LinkedHashSet<>();
        Set<Block> tips = new HashSet<>();
        Set<Block> smashed = new LinkedHashSet<>();
        Map<Block, Boolean> placeable = new HashMap<>();
        Map<Integer, List<Slice>> shapes = new HashMap<>();

        for (int c = 0; c < columns; c++) {
            double side = -width / 2 + c * STEP;
            double edge = Math.abs(side) / (width / 2);
            int columnHeight = (int) Math.round(height * (1 - edge * edge * edge * edge));
            if (columnHeight < 1) {
                continue;
            }
            Location foot = front.clone().add(across.clone().multiply(side));
            // Baja de golpe y sube por cuestas de hasta climb por paso. Un escalón más alto (muro, tronco)
            // no lo trepa: se estrella contra él. Si sigue más alto 3 pasos seguidos es terreno y sube.
            int surface = ground(foot) + 1;
            if (surface <= bases[c] + climb || ++blocked[c] >= 3) {
                bases[c] = surface;
                blocked[c] = 0;
            }
            int base = bases[c];

            for (Slice slice : shapes.computeIfAbsent(columnHeight, h -> profile(h, curl, back))) {
                Location at = foot.clone().add(direction.clone().multiply(slice.forward()));
                int bottom = base + slice.bottom();
                if (slice.forward() < 0) {
                    bottom = Math.max(ground(at) + 1, bottom);
                }
                Block floor = world.getBlockAt(at.getBlockX(), base, at.getBlockZ());
                boolean ok = damage.enabled() && placeable.computeIfAbsent(floor, damage::canPlace);
                for (int y = bottom; y <= base + slice.top(); y++) {
                    Block block = world.getBlockAt(at.getBlockX(), y, at.getBlockZ());
                    if (next.contains(block)) {
                        continue;
                    }
                    if (!ok) {
                        if (y % 3 == 0) {
                            // Sin poder poner agua (zona segura, claim, destrucción apagada) la ola se ve igual.
                            world.spawnParticle(Particle.FALLING_WATER, block.getLocation().add(0.5, 0.5, 0.5), 3,
                                    0.4, 0.4, 0.4, 0, null, true);
                        }
                        continue;
                    }
                    Material type = block.getType();
                    if (!type.isAir() && !water.contains(block)) {
                        if (block.isLiquid()) {
                            continue; // el mar de verdad no se toca
                        }
                        // Solo el frente rompe; el lomo pasa por encima de lo que quede.
                        boolean soft = breakSoft && !type.isSolid();
                        if (slice.forward() < 0 || !(soft || breakable.contains(type))
                                || damage.filter(block, List.of(block)).isEmpty()) {
                            continue;
                        }
                        smashed.add(block);
                    }
                    next.add(block);
                    if (slice.tip()) {
                        tips.add(block);
                    }
                }
            }
        }

        // Lo que el frente arrasa: se quita (se repone luego) y parte sale volando por delante.
        for (Block block : smashed) {
            BlockData was = block.getBlockData();
            damage.remove(block, regenerate());
            Location center = block.getLocation().add(0.5, 0.5, 0.5);
            world.spawnParticle(Particle.BLOCK, center, 8, 0.4, 0.4, 0.4, 0, was, true);
            if (was.getMaterial().isSolid() && debris.size() < maxDebris && random() < debrisChance) {
                FallingBlock flying = engine.fallingBlock(center, was, null);
                flying.setVelocity(direction.clone().multiply(0.6 + random() * 0.6).setY(0.35 + random() * 0.4));
                debris.add(flying);
            }
        }
        if (!smashed.isEmpty()) {
            sound("smash-sound", "ENTITY_ZOMBIE_BREAK_WOODEN_DOOR 2 0.6").play(world, smashed.iterator().next().getLocation());
        }

        Set<Block> gone = new HashSet<>(water);
        gone.removeAll(next);
        leave(gone);
        for (Block block : next) {
            BlockData data = tips.contains(block) ? falling : body;
            if (!water.contains(block) || !block.getBlockData().equals(data)) {
                damage.change(block, data, SAFETY_SECONDS);
            }
        }
        water = next;

        List<ParticleLook> crest = particles("crest-particles", List.of("SPLASH*6", "BUBBLE_POP*2"));
        for (Block block : tips) {
            if (random() < 0.3) {
                for (ParticleLook look : crest) {
                    look.spawn(world, block.getLocation().add(0.5, 1, 0.5), 6, 0.6, 0.3, 0.6, 0.05);
                }
            }
        }
        if (step % 8 == 0) {
            sound("sound", "WEATHER_RAIN_ABOVE 4 0.5").play(world, front);
            sound("roar-sound", "ENTITY_PLAYER_SPLASH_HIGH_SPEED 4 0.5").play(world, front);
        }
    }

    /**
     * Quita el agua que ya no es de la ola. La que queda tocando el suelo puede quedarse de charco
     * ({@code flood-chance}) hasta {@code flood-seconds}.
     */
    private void leave(Set<Block> blocks) {

        double floodChance = number("flood-chance", 0.3);
        double floodSeconds = number("flood-seconds", 45);
        WorldDamage damage = engine.damage();
        BlockData puddle = waterLevel(6);
        for (Block block : blocks) {
            if (block.getType() != Material.WATER) {
                continue;
            }
            boolean onGround = block.getRelative(0, -1, 0).getType().isSolid();
            if (onGround && floodSeconds > 0 && random() < floodChance) {
                damage.change(block, puddle, floodSeconds);
            } else {
                block.setBlockData(Material.AIR.createBlockData(), false);
            }
        }
    }

    /** Agua que no es fuente: si algo la actualiza, se seca en vez de extenderse. 8 = cayendo. */
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
        double width = scaled("width", 40);
        double height = scaled("height", 12);
        double reach = height * number("curl", 0.65);
        Location front = origin.clone().add(direction.clone().multiply(travelled));
        double box = width / 2 + reach + 2;
        double push = strength("push", 1.1);
        double damageAmount = strength("damage", 4);
        int air = (int) Math.round(strength("drown", 60));
        int cooldown = (int) number("hit-cooldown", 20);

        for (Entity entity : world.getNearbyEntities(front, box, height + 4, box)) {
            if (entity instanceof FallingBlock falling && debris.contains(falling)) {
                continue;
            }
            Vector offset = entity.getLocation().toVector().subtract(front.toVector());
            double along = offset.dot(direction);
            double sideways = Math.abs(offset.getX() * -direction.getZ() + offset.getZ() * direction.getX());
            if (along < -4 || along > reach + 1.5 || sideways > width / 2 || !hits(entity)) {
                continue;
            }

            entity.setVelocity(direction.clone().multiply(push).setY(Math.max(0.2, number("lift", 0.35))));
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
        leave(water);
        water = new HashSet<>();
        debris.forEach(engine::discard);
        debris.clear();
        super.cleanup();
    }

}
