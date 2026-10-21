package com.sack.rpgroll.disasters.engine;

import org.bukkit.Bukkit;
import org.bukkit.ExplosionResult;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Los bloques que cambian los desastres de verdad (el tornado arranca, el terremoto abre grietas,
 * los meteoritos hacen cráteres...). Antes de tocar uno mira, en este orden: que la destrucción
 * esté activa, la zona segura, los claims de GriefPrevention, que no guarde nada (cofres, carteles,
 * camas...), los bloques inmunes, la resistencia de su material y, al final, lo que otros plugins
 * (WorldGuard...) protegen de las explosiones.
 * <p>
 * Lo cambiado se apunta con lo que había y se repone pasados {@code regenerate-seconds} (0 = se
 * queda así), poco a poco y de abajo arriba. Solo se repone si sigue como lo dejó el desastre, o
 * vacío, o con agua o lava que se coló: lo que haya construido alguien en el hueco se respeta. La
 * lista se guarda en {@code regen.dat}, así que tras un reinicio o una caída se repone igual.
 */
public final class WorldDamage {

    /** Dónde: por nombre de mundo, que puede no estar cargado aún al leer {@code regen.dat}. */
    record Spot(String world, int x, int y, int z) {

        static Spot of(Block block) {
            return new Spot(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }

        Block block() {
            World world = Bukkit.getWorld(this.world);
            return world == null ? null : world.getBlockAt(x, y, z);
        }
    }

    /** Lo que había, lo que dejó el desastre y cuándo toca reponerlo (ms); due 0 = nunca. */
    record Change(BlockData original, BlockData left, long due) {
    }

    private final Plugin plugin;
    private final Path journal;
    private final Consumer<String> warn;
    private final Predicate<Location> safeZone;
    private final Map<Spot, Change> changes = new LinkedHashMap<>();
    private final Map<Material, Double> resistance = new EnumMap<>(Material.class);
    private Optional<Predicate<Location>> claimLookup;
    private BukkitTask task;
    private boolean dirty;

    private boolean enabled = true;
    private boolean regenerate = true;
    private double regenerateSeconds = 300;
    private double regenerateSpreadSeconds = 60;
    private int restorePerTick = 40;
    private boolean protectContainers = true;
    private int maxChanges = 60000;

    WorldDamage(Plugin plugin, Consumer<String> warn, Predicate<Location> safeZone) {
        this.plugin = plugin;
        this.journal = plugin.getDataFolder().toPath().resolve("regen.dat");
        this.warn = warn;
        this.safeZone = safeZone;
    }

    /** Lee la sección {@code world-damage} de config.yml. */
    void configure(ConfigurationSection section) {

        resistance.clear();
        if (section == null) {
            return;
        }

        enabled = section.getBoolean("enabled", enabled);
        regenerate = section.getBoolean("regenerate", regenerate);
        regenerateSeconds = Math.max(0, section.getDouble("regenerate-seconds", regenerateSeconds));
        regenerateSpreadSeconds = Math.max(0, section.getDouble("regenerate-spread-seconds", regenerateSpreadSeconds));
        restorePerTick = Math.max(1, section.getInt("restore-per-tick", restorePerTick));
        protectContainers = section.getBoolean("protect-containers", protectContainers);
        maxChanges = Math.max(0, section.getInt("max-changes", maxChanges));

        for (String name : section.getStringList("immune")) {
            addResistance(name, 1);
        }
        ConfigurationSection values = section.getConfigurationSection("resistance");
        if (values != null) {
            for (String name : values.getKeys(false)) {
                addResistance(name, values.getDouble(name));
            }
        }
    }

    private void addResistance(String name, double value) {
        try {
            for (Material material : Looks.materials(name)) {
                // La inmunidad de la lista manda sobre un valor más bajo puesto en resistance.
                resistance.merge(material, Math.clamp(value, 0, 1), Math::max);
            }
        } catch (IllegalArgumentException e) {
            warn.accept("config.yml world-damage: " + e.getMessage() + "; se omite");
        }
    }

    boolean enabled() {
        return enabled;
    }

    // ----- Romper -----

    /**
     * De estos bloques, los que el desastre puede cambiar ahora mismo. La resistencia se tira al azar
     * cada vez: un bloque con 0.7 aguanta 7 de cada 10 intentos.
     */
    List<Block> filter(Block source, Collection<Block> candidates) {

        if (!enabled || candidates.isEmpty() || (maxChanges > 0 && changes.size() >= maxChanges)) {
            return List.of();
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Block> allowed = new ArrayList<>();
        for (Block block : candidates) {
            if (block.getType().isAir() || safeZone.test(block.getLocation()) || inClaim(block)
                    || (protectContainers && block.getState(false) instanceof TileState)) {
                continue;
            }
            double resists = resistance.getOrDefault(block.getType(), 0.0);
            if (resists >= 1 || (resists > 0 && random.nextDouble() < resists)) {
                continue;
            }
            allowed.add(block);
        }
        return askProtections(source, allowed);
    }

    /**
     * Si el desastre puede poner algo en este hueco vacío (nieve encima del suelo): mismas reglas que
     * {@link #filter} salvo la resistencia, que es de lo que se rompe.
     */
    boolean canPlace(Block target) {
        return enabled && (maxChanges <= 0 || changes.size() < maxChanges) && !safeZone.test(target.getLocation())
                && !inClaim(target) && !askProtections(target, List.of(target)).isEmpty();
    }

    /**
     * Una explosión de prueba para los demás plugins de protección. No hay una API común para
     * preguntarles sin un jugador, y todos protegen sus zonas de las explosiones.
     * <p>
     * A GriefPrevention no se le pasa porque con su configuración de serie frena las explosiones en
     * toda la superficie, haya claim o no (sus claims ya se miraron antes). Tampoco a los de prioridad
     * MONITOR: solo miran, y CoreProtect la apuntaría como una explosión que no pasó.
     */
    private List<Block> askProtections(Block source, List<Block> blocks) {

        if (blocks.isEmpty()) {
            return blocks;
        }

        BlockExplodeEvent test = new BlockExplodeEvent(source, source.getState(), new ArrayList<>(blocks), 0f,
                ExplosionResult.DESTROY);
        for (RegisteredListener listener : test.getHandlers().getRegisteredListeners()) {
            if (listener.getPriority() == EventPriority.MONITOR
                    || listener.getPlugin().getName().equals("GriefPrevention")) {
                continue;
            }
            try {
                listener.callEvent(test);
            } catch (EventException | RuntimeException e) {
                warn.accept("la comprobación de protecciones de " + listener.getPlugin().getName() + " falló (" + e
                        + "); los desastres no romperán bloques");
                return List.of();
            }
        }
        return test.isCancelled() ? List.of() : List.copyOf(test.blockList());
    }

    /** Si el bloque está en un claim de GriefPrevention. Por reflexión, para no exigirlo; sin él, false. */
    private boolean inClaim(Block block) {

        if (claimLookup == null) {
            claimLookup = Optional.empty();
            Plugin griefPrevention = Bukkit.getPluginManager().getPlugin("GriefPrevention");
            if (griefPrevention != null && griefPrevention.isEnabled()) {
                try {
                    ClassLoader loader = griefPrevention.getClass().getClassLoader();
                    Class<?> main = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention", true, loader);
                    Class<?> claim = Class.forName("me.ryanhamshire.GriefPrevention.Claim", true, loader);
                    Object store = main.getField("dataStore").get(main.getField("instance").get(null));
                    Method getClaimAt = store.getClass().getMethod("getClaimAt", Location.class, boolean.class, claim);
                    claimLookup = Optional.of(location -> {
                        try {
                            return getClaimAt.invoke(store, location, true, null) != null;
                        } catch (ReflectiveOperationException e) {
                            throw new IllegalStateException(e);
                        }
                    });
                } catch (ReflectiveOperationException | RuntimeException e) {
                    warn.accept("no se pudo hablar con GriefPrevention (" + e + "); los desastres no romperán bloques");
                    claimLookup = Optional.of(location -> true);
                }
            }
        }
        return claimLookup.map(lookup -> lookup.test(block.getLocation())).orElse(false);
    }

    /**
     * Cambia el bloque (sin físicas, así nada suelta objetos ni se cae en cadena) y lo apunta para
     * reponerlo a los {@code seconds} segundos (0 = se queda así; ver {@link Phenomenon#regenerate}).
     * Pasa antes por {@link #filter}: aquí ya no se comprueba nada.
     */
    void change(Block block, BlockData to, double seconds) {

        Spot spot = Spot.of(block);
        Change previous = changes.get(spot);
        BlockData original = previous != null ? previous.original() : block.getBlockData();
        block.setBlockData(to, false);

        if (seconds <= 0) {
            if (previous != null) {
                changes.put(spot, new Change(original, to, previous.due()));
                dirty = true;
            }
            return;
        }
        long due = System.currentTimeMillis() + (long) (seconds * 1000);
        changes.put(spot, new Change(original, to, previous != null ? Math.max(previous.due(), due) : due));
        dirty = true;
    }

    void remove(Block block, double seconds) {
        change(block, Material.AIR.createBlockData(), seconds);
    }

    boolean regenerates() {
        return regenerate;
    }

    double regenerateSeconds() {
        return regenerateSeconds;
    }

    double regenerateSpreadSeconds() {
        return regenerateSpreadSeconds;
    }

    /** Si este bloque lo cambió un desastre y aún no se ha repuesto. */
    boolean changed(Block block) {
        return changes.containsKey(Spot.of(block));
    }

    int pending() {
        return changes.size();
    }

    // ----- Reponer -----

    /** Lee {@code regen.dat} (lo que quedó sin reponer en la última sesión) y empieza a reponer. */
    void start() {

        load();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    private int ticksSinceSave;

    private void tick() {
        restoreDue(System.currentTimeMillis(), restorePerTick * 10);
        if (dirty && (ticksSinceSave += 10) >= 600) {
            save();
        }
    }

    /** Repone ya todo lo pendiente (al apagar el servidor o con el comando). Devuelve cuántos. */
    int restoreAll() {
        int restored = restoreDue(Long.MAX_VALUE, Integer.MAX_VALUE);
        save();
        return restored;
    }

    /** Al apagar no se repone nada: se guarda la lista y se sigue al arrancar, cada bloque a su hora. */
    void shutdown() {
        if (task != null) {
            task.cancel();
        }
        save();
    }

    /** De abajo arriba, para que nada repuesto quede flotando sobre un hueco que aún no lo está. */
    private int restoreDue(long now, int limit) {

        List<Map.Entry<Spot, Change>> due = new ArrayList<>();
        for (var entry : changes.entrySet()) {
            Change change = entry.getValue();
            if (change.due() > 0 && change.due() <= now && Bukkit.getWorld(entry.getKey().world()) != null) {
                due.add(entry);
            }
        }
        if (due.isEmpty()) {
            return 0;
        }

        due.sort(Comparator.comparingInt(entry -> entry.getKey().y()));
        int restored = 0;
        for (var entry : due) {
            if (restored >= limit) {
                break;
            }
            changes.remove(entry.getKey());
            dirty = true;
            Block block = entry.getKey().block();
            if (block != null && restore(block, entry.getValue())) {
                restored++;
            }
        }
        return restored;
    }

    private boolean restore(Block block, Change change) {

        BlockData now = block.getBlockData();
        if (!now.matches(change.left()) && !now.getMaterial().isAir() && !block.isLiquid()) {
            return false;
        }
        block.setBlockData(change.original(), false);

        // Quien estaba dentro del hueco sale por arriba en vez de quedar atrapado.
        if (change.original().getMaterial().isSolid()) {
            Location center = block.getLocation().add(0.5, 0.5, 0.5);
            for (Entity entity : block.getWorld().getNearbyEntities(center, 0.5, 0.5, 0.5)) {
                if (entity instanceof LivingEntity) {
                    Location out = entity.getLocation();
                    out.setY(block.getWorld().getHighestBlockYAt(out.getBlockX(), out.getBlockZ(),
                            HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1);
                    entity.teleport(out);
                }
            }
        }
        return true;
    }

    // ----- regen.dat -----

    /** Una línea por bloque: mundo, x, y, z, cuándo (ms), lo que había y lo que dejó, separado por tabuladores. */
    private void save() {

        dirty = false;
        ticksSinceSave = 0;
        List<String> lines = new ArrayList<>(changes.size());
        changes.forEach((spot, change) -> lines.add(String.join("\t", spot.world(), String.valueOf(spot.x()),
                String.valueOf(spot.y()), String.valueOf(spot.z()), String.valueOf(change.due()),
                change.original().getAsString(), change.left().getAsString())));

        Runnable write = () -> {
            try {
                if (lines.isEmpty()) {
                    Files.deleteIfExists(journal);
                    return;
                }
                Files.createDirectories(journal.getParent());
                Path temp = journal.resolveSibling("regen.dat.tmp");
                Files.write(temp, lines, StandardCharsets.UTF_8);
                Files.move(temp, journal, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                plugin.getLogger().warning("✘ No se pudo guardar regen.dat: " + e.getMessage());
            }
        };
        // Al apagar no se puede programar nada: se escribe ya.
        if (plugin.isEnabled() && !Bukkit.isStopping()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, write);
        } else {
            write.run();
        }
    }

    private void load() {

        if (!Files.exists(journal)) {
            return;
        }
        try {
            int bad = 0;
            for (String line : Files.readAllLines(journal, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\t");
                try {
                    Spot spot = new Spot(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3]));
                    changes.put(spot, new Change(Bukkit.createBlockData(parts[5]), Bukkit.createBlockData(parts[6]),
                            Long.parseLong(parts[4])));
                } catch (RuntimeException e) {
                    bad++;
                }
            }
            plugin.getLogger().info("✔ regen.dat: " + changes.size() + " bloque(s) de desastres por reponer"
                    + (bad > 0 ? " (" + bad + " línea(s) ilegibles omitidas)" : "") + ".");
        } catch (IOException e) {
            plugin.getLogger().warning("✘ No se pudo leer regen.dat: " + e.getMessage());
        }
    }

}
