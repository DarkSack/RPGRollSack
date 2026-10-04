package com.sack.rpgroll.machines.spawner;

import com.sack.rpgroll.machines.core.Cost;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * spawners.yml ya leído. Cada mejora es una lista de niveles; el primero (nivel 0) es como
 * queda un spawner sin mejorar y no tiene coste.
 */
public record SpawnerSettings(boolean enabled, Set<String> disabledWorlds, boolean mine, boolean mineNeedsSilk,
        boolean stack, int maxStack, boolean hologram, double hologramHeight, Map<Upgrade, Track> tracks) {

    /** Las tres mejoras. */
    public enum Upgrade {
        SPEED("speed", Material.SUGAR),
        COUNT("count", Material.SPAWNER),
        RANGE("range", Material.ENDER_EYE);

        private final String id;
        private final Material icon;

        Upgrade(String id, Material icon) {
            this.id = id;
            this.icon = icon;
        }

        public String id() {
            return id;
        }

        public Material icon() {
            return icon;
        }
    }

    /**
     * Un nivel. Según la mejora se usan unos valores u otros: velocidad, {@code minDelay} y
     * {@code maxDelay}; cantidad, {@code spawnCount} y {@code maxNearby}; alcance, {@code range}.
     */
    public record Level(int minDelay, int maxDelay, int spawnCount, int maxNearby, int range, Cost cost) {
    }

    public record Track(Material icon, List<Level> levels) {

        public Level level(int index) {
            return levels.get(Math.clamp(index, 0, levels.size() - 1));
        }

        public int max() {
            return levels.size() - 1;
        }
    }

    /** Lo que trae un spawner vanilla. */
    static final Level VANILLA = new Level(200, 800, 4, 6, 16, Cost.FREE);

    public static SpawnerSettings from(FileConfiguration config, Consumer<String> warn) {

        java.util.EnumMap<Upgrade, Track> tracks = new java.util.EnumMap<>(Upgrade.class);
        for (Upgrade upgrade : Upgrade.values()) {
            ConfigurationSection section = config.getConfigurationSection("upgrades." + upgrade.id());
            List<Level> levels = new ArrayList<>();
            if (section != null && section.getBoolean("enabled", true)) {
                for (Map<?, ?> raw : section.getMapList("levels")) {
                    Level level = level(raw, warn, upgrade);
                    if (level != null) {
                        levels.add(level);
                    }
                }
            }
            if (levels.isEmpty()) {
                levels.add(VANILLA);
            }
            Material icon = Material.matchMaterial(section == null ? "" : section.getString("icon", ""));
            tracks.put(upgrade, new Track(icon == null || !icon.isItem() ? upgrade.icon() : icon, List.copyOf(levels)));
        }

        return new SpawnerSettings(config.getBoolean("enabled", true),
                config.getStringList("disabled-worlds").stream().map(w -> w.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toUnmodifiableSet()),
                config.getBoolean("mine.enabled", true),
                config.getBoolean("mine.require-silk-touch", true),
                config.getBoolean("stack.enabled", true),
                Math.max(1, config.getInt("stack.max", 8)),
                config.getBoolean("hologram.enabled", true),
                config.getDouble("hologram.height", 1.35),
                java.util.Collections.unmodifiableMap(tracks));
    }

    private static Level level(Map<?, ?> raw, Consumer<String> warn, Upgrade upgrade) {

        int min = integer(raw, "min-delay", VANILLA.minDelay());
        int max = integer(raw, "max-delay", VANILLA.maxDelay());
        if (min < 1 || max < min) {
            warn.accept("spawners.yml (" + upgrade.id() + "): min-delay debe ser >= 1 y <= max-delay; nivel ignorado");
            return null;
        }
        ConfigurationSection cost = null;
        Object costRaw = raw.get("cost");
        if (costRaw instanceof Map<?, ?> map) {
            org.bukkit.configuration.MemoryConfiguration memory = new org.bukkit.configuration.MemoryConfiguration();
            cost = memory.createSection("cost", map);
        }
        return new Level(min, max,
                Math.max(1, integer(raw, "spawn-count", VANILLA.spawnCount())),
                Math.max(1, integer(raw, "max-nearby", VANILLA.maxNearby())),
                Math.max(1, integer(raw, "range", VANILLA.range())),
                Cost.from(cost, text -> warn.accept("spawners.yml (" + upgrade.id() + "): " + text)));
    }

    private static int integer(Map<?, ?> raw, String key, int fallback) {
        Object value = raw.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString().trim());
            } catch (NumberFormatException ignored) {
                // cae al valor por defecto
            }
        }
        return fallback;
    }

    public Track track(Upgrade upgrade) {
        return tracks.get(upgrade);
    }

    public boolean allowed(String world) {
        return enabled && !disabledWorlds.contains(world.toLowerCase(Locale.ROOT));
    }
}
