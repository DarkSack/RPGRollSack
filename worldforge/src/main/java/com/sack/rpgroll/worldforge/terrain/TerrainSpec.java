package com.sack.rpgroll.worldforge.terrain;

import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.api.Result;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A terrain shape, in the style of Tectonic or Terralith: vanilla's own noise router with its
 * numbers changed (taller or flatter land, wider continents, sharper peaks, bigger biomes).
 * {@link TerrainWriter} turns it into datapack files; nothing here touches the server.
 *
 * @param heightScale   vertical stretch around y=64: 2 doubles every hill and every valley
 * @param heightOffset  blocks the whole land is raised (or sunk, if negative)
 * @param peaks         multiplies vanilla's jagged peaks; 0 removes them
 * @param continentSize horizontal size of land and ocean masses (2 = twice as wide)
 */
public record TerrainSpec(String id, String description, String base, double heightScale, int heightOffset, double peaks,
                          double continentSize, double erosionSize, double ridgeSize, double biomeSize, int seaLevel,
                          boolean aquifers, boolean oreVeins) {

    public static final Set<String> BASES = Set.of("overworld", "amplified", "large_biomes");
    public static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_]{0,23}");
    private static final Set<String> KEYS = Set.of("id", "description", "base", "height-scale", "height-offset", "peaks",
            "continent-size", "erosion-size", "ridge-size", "biome-size", "sea-level", "aquifers", "ore-veins");

    /** Where the land starts being flattened in a dimension whose top is {@code top} (vanilla: 240, amplified: 304). */
    public int slideStart(int top) {
        return top - (base.equals("amplified") ? 16 : 80);
    }

    /** Roughly the highest surface: vanilla land tops out where its top slide starts. */
    public int estimatedPeak() {
        return (int) Math.round(64 + heightScale * (slideStart(320) - 64) + heightOffset);
    }

    public static Result<TerrainSpec> parse(String yaml) {
        YamlConfiguration doc = new YamlConfiguration();
        try {
            doc.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            return Result.fail("", "YAML no válido: " + e.getMessage().lines().findFirst().orElse(""));
        }
        List<Problem> p = new ArrayList<>();
        for (String k : doc.getKeys(false)) {
            if (!KEYS.contains(k)) {
                p.add(Problem.error(k, "clave desconocida; valen: " + String.join(", ", KEYS.stream().sorted().toList()) + "."));
            }
        }
        String id = doc.getString("id");
        if (id == null || !ID.matcher(id).matches()) {
            p.add(Problem.error("id", "«" + id + "» no vale: minúsculas, números o '_', hasta 24."));
        }
        String description = doc.getString("description", "");
        if (description.length() > 120) {
            p.add(Problem.error("description", "como mucho 120 caracteres."));
        }
        String base = doc.getString("base", "overworld");
        if (!BASES.contains(base)) {
            p.add(Problem.error("base", "«" + base + "» no vale; hay: overworld, amplified, large_biomes."));
        }
        double heightScale = number(doc, "height-scale", 1, 0.25, 3, p);
        double heightOffset = number(doc, "height-offset", 0, -96, 96, p);
        double peaks = number(doc, "peaks", 1, 0, 4, p);
        double continent = number(doc, "continent-size", 1, 0.25, 8, p);
        double erosion = number(doc, "erosion-size", 1, 0.25, 8, p);
        double ridge = number(doc, "ridge-size", 1, 0.25, 8, p);
        double biome = number(doc, "biome-size", 1, 0.25, 8, p);
        double sea = number(doc, "sea-level", 63, -64, 256, p);
        boolean aquifers = flag(doc, "aquifers", p);
        boolean veins = flag(doc, "ore-veins", p);
        if (p.stream().anyMatch(Problem::isError)) {
            return Result.fail(p);
        }
        return Result.ok(new TerrainSpec(id, description, base, heightScale, (int) heightOffset, peaks, continent, erosion,
                ridge, biome, (int) sea, aquifers, veins));
    }

    private static double number(YamlConfiguration doc, String key, double def, double min, double max, List<Problem> p) {
        Object v = doc.get(key);
        if (v == null) {
            return def;
        }
        if (!(v instanceof Number n)) {
            p.add(Problem.error(key, "tiene que ser un número."));
            return def;
        }
        double d = n.doubleValue();
        if (!(d >= min && d <= max)) {
            p.add(Problem.error(key, d + " fuera de rango: de " + fmt(min) + " a " + fmt(max) + "."));
        }
        return d;
    }

    private static boolean flag(YamlConfiguration doc, String key, List<Problem> p) {
        Object v = doc.get(key);
        if (v == null) {
            return true;
        }
        if (!(v instanceof Boolean b)) {
            p.add(Problem.error(key, "tiene que ser true o false."));
            return true;
        }
        return b;
    }

    static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
