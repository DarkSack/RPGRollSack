package com.sack.rpgroll.mobs.size;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Ajustes de {@code random-size} del config.yml: qué mobs cambian de tamaño
 * al aparecer, entre qué tamaños y cuánto siguen al tamaño su vida, daño,
 * velocidad, experiencia y botín.
 * <p>
 * El tamaño sale de una distribución triangular dentro del rango (los
 * tamaños del centro son los más comunes) y, con {@code extremes.chance} %,
 * un mini o un gigante fuera del rango. Un tipo con rango propio en
 * {@code types} no tiene extremos.
 * <p>
 * Vale para los mobs vanilla y para los de RPGRoll ({@code rpgroll-mobs}); a
 * estos se les puede dar rango propio en {@code types} como
 * {@code "rpgroll:<id>"}. Los jefes y los que llevan modelo 3D no cambian
 * (el modelo no sigue al tamaño), salvo {@code rpgroll-modeled: true}.
 */
public record RandomSizeSettings(
        boolean enabled,
        boolean rpgrollMobs,
        boolean rpgrollModeled,
        Set<String> disabledWorlds,
        Set<String> spawnReasons,
        Set<String> excludedTypes,
        Map<Category, Range> ranges,
        Map<String, Range> types,
        double extremeChance,
        double extremeSmall,
        double extremeLarge,
        double hostileMinHeight,
        double healthWeight,
        double damageWeight,
        double speedWeight,
        double experienceWeight,
        double lootWeight) {

    public enum Category { PASSIVE, NEUTRAL, HOSTILE }

    public record Range(double min, double max) {

        public Range {
            if (max < min) {
                double swap = min;
                min = max;
                max = swap;
            }
            min = Math.max(0.0625, min);
            max = Math.min(16, max);
        }
    }

    public static RandomSizeSettings disabled() {
        return new RandomSizeSettings(false, false, false, Set.of(), Set.of(), Set.of(), Map.of(), Map.of(),
                0, 1, 1, 0, 0, 0, 0, 0, 0);
    }

    public static RandomSizeSettings from(ConfigurationSection section) {

        if (section == null) {
            return disabled();
        }

        Map<Category, Range> ranges = new HashMap<>();
        ranges.put(Category.PASSIVE, range(section.getConfigurationSection("ranges.passive"), 0.75, 1.25));
        ranges.put(Category.NEUTRAL, range(section.getConfigurationSection("ranges.neutral"), 0.8, 1.25));
        ranges.put(Category.HOSTILE, range(section.getConfigurationSection("ranges.hostile"), 0.85, 1.3));

        Map<String, Range> types = new HashMap<>();
        ConfigurationSection typesSection = section.getConfigurationSection("types");
        if (typesSection != null) {
            for (String key : typesSection.getKeys(false)) {
                types.put(normalize(key), range(typesSection.getConfigurationSection(key), 1, 1));
            }
        }

        return new RandomSizeSettings(
                section.getBoolean("enabled", false),
                section.getBoolean("rpgroll-mobs", true),
                section.getBoolean("rpgroll-modeled", false),
                lower(section.getStringList("disabled-worlds")),
                upper(section.getStringList("spawn-reasons")),
                lower(section.getStringList("exclude-types")),
                ranges,
                types,
                section.getDouble("extremes.chance", 0),
                section.getDouble("extremes.small", 0.5),
                section.getDouble("extremes.large", 1.8),
                section.getDouble("hostile-min-height", 1.05),
                section.getDouble("stats.health", 1),
                section.getDouble("stats.damage", 0.5),
                section.getDouble("stats.speed", 0),
                section.getDouble("stats.experience", 1),
                section.getDouble("stats.loot", 0));
    }

    private static Range range(ConfigurationSection section, double min, double max) {
        if (section == null) {
            return new Range(min, max);
        }
        return new Range(section.getDouble("min", min), section.getDouble("max", max));
    }

    private static Set<String> lower(List<String> values) {
        Set<String> result = new HashSet<>();
        values.forEach(value -> result.add(normalize(value)));
        return result;
    }

    private static Set<String> upper(List<String> values) {
        Set<String> result = new HashSet<>();
        values.forEach(value -> result.add(value.trim().toUpperCase(Locale.ROOT)));
        return result;
    }

    /** "minecraft:Zombie", "ZOMBIE" y "zombie" son el mismo tipo. */
    static String normalize(String type) {
        String value = type.trim().toLowerCase(Locale.ROOT);
        return value.startsWith("minecraft:") ? value.substring("minecraft:".length()) : value;
    }

    public boolean appliesTo(String world, String spawnReason, String type) {
        return enabledIn(world)
                && spawnReasons.contains(spawnReason.toUpperCase(Locale.ROOT))
                && !excludedTypes.contains(normalize(type));
    }

    public boolean enabledIn(String world) {
        return enabled && !disabledWorlds.contains(normalize(world));
    }

    /**
     * El tamaño de un mob nuevo. {@code height} es su alto a tamaño 1: un
     * hostil alto nunca baja de {@code hostileMinHeight}, para que no se cuele
     * por huecos de un bloque que antes le cerraban el paso.
     */
    public double pick(Category category, String type, double height, RandomGenerator random) {

        Range own = types.get(normalize(type));
        double scale;

        if (own == null && extremeChance > 0 && random.nextDouble() * 100 < extremeChance) {
            scale = random.nextBoolean() ? extremeSmall : extremeLarge;
        } else {
            Range range = own != null ? own : ranges.getOrDefault(category, new Range(1, 1));
            double t = (random.nextDouble() + random.nextDouble()) / 2;
            scale = range.min() + (range.max() - range.min()) * t;
        }

        if (category == Category.HOSTILE && height >= hostileMinHeight && height * scale < hostileMinHeight) {
            scale = hostileMinHeight / height;
        }

        return scale;
    }

    /** Cuánto cambia un valor que sigue al tamaño con peso {@code weight}: 1 + (tamaño − 1) · peso, nunca negativo. */
    public static double factor(double scale, double weight) {
        return Math.max(0.05, 1 + (scale - 1) * weight);
    }

}
