package com.sack.rpgroll.worldforge.generation;

import com.sack.rpgroll.worldforge.terrain.TerrainSpec;

import org.bukkit.util.noise.SimplexOctaveGenerator;

import java.util.List;

/**
 * WorldForge's own terrain, for worlds created while the server runs (a datapack terrain only
 * reaches dimensions and the main world, read at startup). Same knobs as {@link TerrainSpec},
 * its own shape: continents, erosion and ridges from Bukkit's simplex noise give a surface
 * height per column; Minecraft's surface, caves, decorations and structures run on top.
 * Biomes are picked from the height and two climate noises, so peaks get mountain biomes and
 * the sea gets ocean ones. Immutable once built: safe from any generation thread.
 */
public final class TerrainEngine {

    /** Every biome {@link #biome} can return. */
    public static final List<String> BIOMES = List.of(
            "minecraft:deep_frozen_ocean", "minecraft:deep_cold_ocean", "minecraft:deep_ocean", "minecraft:deep_lukewarm_ocean",
            "minecraft:frozen_ocean", "minecraft:cold_ocean", "minecraft:ocean", "minecraft:lukewarm_ocean", "minecraft:warm_ocean",
            "minecraft:snowy_beach", "minecraft:beach", "minecraft:swamp", "minecraft:mangrove_swamp",
            "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:stony_peaks",
            "minecraft:snowy_slopes", "minecraft:grove", "minecraft:meadow", "minecraft:cherry_grove",
            "minecraft:windswept_hills", "minecraft:windswept_forest", "minecraft:windswept_savanna",
            "minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:taiga", "minecraft:old_growth_spruce_taiga",
            "minecraft:plains", "minecraft:forest", "minecraft:birch_forest", "minecraft:flower_forest", "minecraft:dark_forest",
            "minecraft:savanna", "minecraft:sparse_jungle", "minecraft:jungle", "minecraft:desert", "minecraft:badlands",
            "minecraft:lush_caves", "minecraft:dripstone_caves");

    private final TerrainSpec spec;
    private final long seed;
    private final int minY, maxY;
    private final SimplexOctaveGenerator continents, erosion, ridges, hills, temperature, humidity;
    private final double amplify;

    public TerrainEngine(TerrainSpec spec, long seed, int minY, int maxY) {
        this.spec = spec;
        this.seed = seed;
        this.minY = minY;
        this.maxY = maxY;
        this.amplify = spec.base().equals("amplified") ? 2 : 1;
        double biomeSize = spec.biomeSize() * (spec.base().equals("large_biomes") ? 4 : 1);
        continents = noise(seed, 1, 1200 * spec.continentSize());
        erosion = noise(seed, 2, 700 * spec.erosionSize());
        ridges = noise(seed, 3, 350 * spec.ridgeSize());
        hills = noise(seed, 4, 90);
        temperature = noise(seed, 5, 900 * biomeSize);
        humidity = noise(seed, 6, 700 * biomeSize);
    }

    private static SimplexOctaveGenerator noise(long seed, int salt, double size) {
        SimplexOctaveGenerator g = new SimplexOctaveGenerator(seed * 31 + salt, 4);
        g.setScale(1 / size);
        return g;
    }

    public long seed() {
        return seed;
    }

    /**
     * In [−1, 1]: the first octave has the generator's size, each next one half as big and half
     * as strong. Four normalized octaves rarely pass ±0.6, hence the stretch.
     */
    private static double n(SimplexOctaveGenerator g, int x, int z) {
        return Math.max(-1, Math.min(1, 1.6 * g.noise(x, z, 2, 0.5, true)));
    }

    /**
     * Land height above or below y=64 before {@code height-scale} and {@code height-offset}:
     * about −34 on the deep sea floor, 0 at the coast, up to ~190 on the highest peaks.
     */
    double relative(int x, int z) {
        double c = n(continents, x, z);
        double e = (n(erosion, x, z) + 1) / 2;          // 0 = rugged, 1 = worn flat
        double r = 1 - Math.abs(n(ridges, x, z));        // 1 on ridge lines
        double base;
        if (c < -0.45) {
            base = lerp(-34, -18, (c + 1) / 0.55);
        } else if (c < -0.15) {
            base = lerp(-18, 2, (c + 0.45) / 0.30);
        } else {
            base = lerp(2, 42, smooth((c + 0.15) / 1.15));
        }
        double mountains = Math.max(0, c + 0.1) * (1 - e);
        double rel = base + mountains * (40 + 100 * r * r * spec.peaks()) + n(hills, x, z) * 6 * (0.4 + (1 - e));
        return rel > 0 ? rel * amplify : rel;
    }

    /** Top solid block at x, z. Above {@code maxY − 64} it is squeezed so it never reaches the ceiling. */
    public int height(int x, int z) {
        return height(relative(x, z));
    }

    private int height(double relative) {
        double h = 64 + spec.heightScale() * relative + spec.heightOffset();
        double soft = maxY - 64, room = 56;
        if (h > soft) {
            h = soft + room * Math.tanh((h - soft) / room);
        }
        return (int) Math.max(minY + 1, Math.min(maxY - 8, Math.round(h)));
    }

    public int seaLevel() {
        return spec.seaLevel();
    }

    /** Biome at a block, as a key: the surface one, or a cave biome deep under it. */
    public String biome(int x, int y, int z) {
        double r = relative(x, z);
        int h = height(r);
        double t = n(temperature, x, z) - Math.max(0, h - 140) / 220.0;   // higher is colder
        double w = n(humidity, x, z);
        if (y < h - 24 && y < 48) {
            if (w > 0.45) {
                return "minecraft:lush_caves";
            }
            if (w < -0.35 && t > 0.1) {
                return "minecraft:dripstone_caves";
            }
        }
        int sea = spec.seaLevel();
        if (h < sea - 18) {
            return t < -0.5 ? "minecraft:deep_frozen_ocean" : t < -0.15 ? "minecraft:deep_cold_ocean"
                    : t < 0.35 ? "minecraft:deep_ocean" : "minecraft:deep_lukewarm_ocean";
        }
        if (h < sea - 1) {
            return t < -0.5 ? "minecraft:frozen_ocean" : t < -0.15 ? "minecraft:cold_ocean" : t < 0.35 ? "minecraft:ocean"
                    : t < 0.6 ? "minecraft:lukewarm_ocean" : "minecraft:warm_ocean";
        }
        if (h <= sea + 3 && w > 0.55) {
            return t > 0.45 ? "minecraft:mangrove_swamp" : "minecraft:swamp";
        }
        if (h <= sea + 2) {
            return t < -0.5 ? "minecraft:snowy_beach" : "minecraft:beach";
        }
        // Mountain biomes by the height before stretching and squeezing, so a taller terrain keeps them on its peaks.
        double rel = r / amplify;
        if (rel > 150) {
            return t < -0.2 ? "minecraft:frozen_peaks" : t < 0.4 ? "minecraft:jagged_peaks" : "minecraft:stony_peaks";
        }
        if (rel > 100) {
            if (w > 0.45 && t > 0.1 && t < 0.5) {
                return "minecraft:cherry_grove";
            }
            return t < -0.3 ? "minecraft:snowy_slopes" : t < 0.1 ? "minecraft:grove" : "minecraft:meadow";
        }
        if (rel > 70) {
            return t > 0.6 ? "minecraft:windswept_savanna" : w > 0.3 ? "minecraft:windswept_forest" : "minecraft:windswept_hills";
        }
        if (t < -0.45) {
            return w < 0 ? "minecraft:snowy_plains" : "minecraft:snowy_taiga";
        }
        if (t < -0.1) {
            return w < 0.35 ? "minecraft:taiga" : "minecraft:old_growth_spruce_taiga";
        }
        if (t < 0.35) {
            return w < -0.35 ? "minecraft:plains" : w < 0.1 ? "minecraft:forest" : w < 0.3 ? "minecraft:birch_forest"
                    : w < 0.5 ? "minecraft:flower_forest" : "minecraft:dark_forest";
        }
        if (t < 0.65) {
            return w < -0.2 ? "minecraft:savanna" : w < 0.2 ? "minecraft:plains" : w < 0.5 ? "minecraft:sparse_jungle" : "minecraft:jungle";
        }
        return w < 0 ? "minecraft:desert" : w < 0.3 ? "minecraft:badlands" : "minecraft:jungle";
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.max(0, Math.min(1, t));
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
