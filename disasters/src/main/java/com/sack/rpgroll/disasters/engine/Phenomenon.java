package com.sack.rpgroll.disasters.engine;

import com.sack.rpgroll.disasters.core.DisasterPhenomenon;
import com.sack.rpgroll.disasters.engine.Looks.ParticleLook;
import com.sack.rpgroll.disasters.engine.Looks.SoundLook;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * Un fenómeno en marcha. Corre cada {@code period} ticks hasta agotar la duración del desastre.
 * <p>
 * Parámetros comunes a todos: {@code period} (ticks entre pasos), {@code storm} y {@code thunder}
 * (lluvia y truenos mientras dura; al terminar se deja el tiempo como estaba), {@code hit-mobs}
 * (si también afecta a los mobs, true), {@code hit-items} (si mueve objetos sueltos, true) y
 * {@code effects} (efectos de poción para cada ser vivo al que golpea, ej. {@code ["SLOWNESS 2 5"]}) y
 * {@code regenerate}, {@code regenerate-seconds} y {@code regenerate-spread-seconds} (si se repone lo
 * que rompa y cuándo; sin ellos, los del desastre y luego los de config.yml).
 * <p>
 * El nivel del desastre llega como {@link Power}: {@link #strength} multiplica por su intensidad
 * (daño, empuje, frecuencia) y {@link #scaled} por su tamaño (radios, alturas, largos).
 */
abstract class Phenomenon extends BukkitRunnable {

    protected final DisasterEngine engine;
    protected final World world;
    protected final DisasterPhenomenon params;
    protected final double intensity;
    protected final double size;
    protected final int period;
    protected int age;
    private int remaining;
    private final boolean stormBefore;
    private final boolean thunderBefore;
    private final Map<String, Object> looks = new HashMap<>();

    /** Lo que el nivel (por el {@code intensity} del desastre) multiplica. */
    record Power(double intensity, double size) {
    }

    Phenomenon(DisasterEngine engine, World world, DisasterPhenomenon params, Power power, int durationTicks,
            int defaultPeriod) {
        this.engine = engine;
        this.world = world;
        this.params = params;
        this.intensity = power.intensity();
        this.size = power.size();
        this.period = (int) Math.max(1, params.number("period", defaultPeriod));
        this.remaining = durationTicks;
        this.stormBefore = world.hasStorm();
        this.thunderBefore = world.isThundering();
    }

    @Override
    public final void run() {
        if (remaining <= 0 || world.getPlayers().isEmpty()) {
            engine.finished(this);
            return;
        }
        if (params.flag("storm", defaultStorm()) && !world.hasStorm()) {
            world.setStorm(true);
        }
        if (params.flag("thunder", false) && !world.isThundering()) {
            world.setThundering(true);
        }
        tick();
        age += period;
        remaining -= period;
    }

    abstract void tick();

    /** Si por defecto trae tormenta (el YAML lo cambia con {@code storm}). */
    boolean defaultStorm() {
        return false;
    }

    /** Quita lo que dejó en el mundo y devuelve el tiempo como estaba. Se llama una vez al terminar. */
    void cleanup() {
        if (params.flag("storm", defaultStorm()) && !stormBefore) {
            world.setStorm(false);
        }
        if (params.flag("thunder", false) && !thunderBefore) {
            world.setThundering(false);
        }
    }

    // ----- Parámetros -----

    double number(String key, double fallback) {
        return params.number(key, fallback);
    }

    /** Un parámetro de fuerza (daño, empuje, probabilidad) ya multiplicado por la intensidad. */
    double strength(String key, double fallback) {
        return params.number(key, fallback) * intensity;
    }

    /** Un parámetro de tamaño (radio, alto, largo) ya multiplicado por el tamaño del nivel. */
    double scaled(String key, double fallback) {
        return params.number(key, fallback) * size;
    }

    /** Para {@link WorldDamage#change}: segundos hasta reponer lo que rompa, con su extra al azar; 0 = nunca. */
    double regenerate() {
        WorldDamage damage = engine.damage();
        double seconds = params.number("regenerate-seconds", damage.regenerateSeconds());
        if (!params.flag("regenerate", damage.regenerates()) || seconds <= 0) {
            return 0;
        }
        return seconds + random() * Math.max(0, params.number("regenerate-spread-seconds", damage.regenerateSpreadSeconds()));
    }

    boolean flag(String key, boolean fallback) {
        return params.flag(key, fallback);
    }

    List<ParticleLook> particles(String key, List<String> fallback) {
        return parsedList(key, fallback, Looks::particle);
    }

    SoundLook sound(String key, String fallback) {
        return parsed(key, fallback, Looks::sound);
    }

    /**
     * Bloques de una lista; {@code NOMBRE*peso} lo hace salir más a menudo en {@link #pick}
     * ({@code ["MAGMA_BLOCK*6", "DEEPSLATE_DIAMOND_ORE"]}: 6 de cada 7).
     */
    @SuppressWarnings("unchecked")
    List<BlockData> blocks(String key, List<String> fallback) {
        Object cached = looks.get(key + "#weighted");
        if (cached == null) {
            List<BlockData> all = new ArrayList<>();
            List<List<BlockData>> parts = parsedList(key, fallback, Looks::weightedBlock);
            parts.forEach(all::addAll);
            cached = List.copyOf(all);
            looks.put(key + "#weighted", cached);
        }
        return (List<BlockData>) cached;
    }

    BlockData block(String key, String fallback) {
        return parsed(key, fallback, name -> Looks.material(name).createBlockData());
    }

    /** Bloques en los que se acepta {@code *} como comodín (p. ej. {@code *_LEAVES}). */
    @SuppressWarnings("unchecked")
    Set<Material> materials(String key, List<String> fallback) {
        Object cached = looks.get(key + "#set");
        if (cached == null) {
            // Fuera de computeIfAbsent: parsedList también escribe en este mapa.
            Set<Material> found = EnumSet.noneOf(Material.class);
            List<List<Material>> parts = parsedList(key, fallback, Looks::materials);
            parts.forEach(found::addAll);
            looks.put(key + "#set", found);
            cached = found;
        }
        return (Set<Material>) cached;
    }

    /** Un elemento al azar; null si la lista está vacía. */
    static <T> T pick(List<T> options) {
        return options.isEmpty() ? null : options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }

    /** Se lee una vez y se guarda; si el YAML trae algo que no se entiende, avisa y usa el de por defecto. */
    @SuppressWarnings("unchecked")
    private <T> T parsed(String key, String fallback, Function<String, T> parser) {
        return (T) looks.computeIfAbsent(key, ignored -> {
            String raw = params.text(key, fallback);
            try {
                return parser.apply(raw);
            } catch (RuntimeException e) {
                engine.warn(params.type() + " '" + key + "': " + raw + " no vale (" + e.getMessage()
                        + "); se usa " + fallback);
                return parser.apply(fallback);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> parsedList(String key, List<String> fallback, Function<String, T> parser) {
        return (List<T>) looks.computeIfAbsent(key, ignored -> {
            List<T> parsedItems = new ArrayList<>();
            for (String raw : params.list(key, fallback)) {
                try {
                    parsedItems.add(parser.apply(raw));
                } catch (RuntimeException e) {
                    engine.warn(params.type() + " '" + key + "': " + raw + " no vale (" + e.getMessage() + "); se omite");
                }
            }
            return List.copyOf(parsedItems);
        });
    }

    // ----- A quién toca -----

    /** Si este fenómeno puede mover o golpear la entidad (además de las reglas de {@link DisasterEngine#affects}). */
    boolean hits(Entity entity) {
        if (!engine.affects(entity)) {
            return false;
        }
        if (entity instanceof Item) {
            return flag("hit-items", true);
        }
        return entity instanceof Player || flag("hit-mobs", true);
    }

    /** Daño (si es mayor que 0) y los efectos de {@code effects}. */
    void strike(LivingEntity living, double damage) {
        if (damage > 0) {
            living.damage(damage);
        }
        for (PotionEffect effect : this.<PotionEffect>parsedList("effects", List.of(), Looks::potion)) {
            living.addPotionEffect(effect);
        }
    }

    static double random() {
        return ThreadLocalRandom.current().nextDouble();
    }

}
