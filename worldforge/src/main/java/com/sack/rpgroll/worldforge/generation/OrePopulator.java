package com.sack.rpgroll.worldforge.generation;

import com.sack.rpgroll.worldforge.profile.OreRule;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Places a profile's ore rules in each chunk as Minecraft generates it. It only touches
 * its {@link LimitedRegion} (the chunk plus a small buffer), so it is safe wherever the
 * server runs population, and it never revisits a chunk that already exists.
 */
final class OrePopulator extends BlockPopulator {

    private record Rule(OreRule source, Material ore, Set<Material> replaces, Set<String> biomes,
                        Set<String> excluded) {
    }

    private static final int[][] STEPS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private final List<Rule> rules;

    OrePopulator(List<OreRule> rules) {
        this.rules = rules.stream().map(r -> new Rule(r, WorldForgeGenerator.material(r.block()),
                r.replaces().stream().map(WorldForgeGenerator::material)
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(Material.class))),
                Set.copyOf(r.biomes()), Set.copyOf(r.excludeBiomes()))).toList();
    }

    @Override
    public void populate(WorldInfo info, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        int worldMin = info.getMinHeight(), worldMax = info.getMaxHeight();
        for (Rule rule : rules) {
            OreRule r = rule.source();
            int minY = Math.max(r.minY(), worldMin), maxY = Math.min(r.maxY(), worldMax - 1);
            if (minY > maxY) {
                continue;
            }
            for (int v = 0; v < r.veinsPerChunk(); v++) {
                int x = (chunkX << 4) + random.nextInt(16);
                int z = (chunkZ << 4) + random.nextInt(16);
                int y = pickY(random, minY, maxY, r.distribution());
                if (!biomeAllowed(rule, region.getBiome(x, y, z))) {
                    continue;
                }
                vein(region, random, rule, x, y, z, worldMin, worldMax);
            }
        }
    }

    private static boolean biomeAllowed(Rule rule, Biome biome) {
        NamespacedKey key = biome.getKey();
        String k = key.toString();
        if (rule.excluded().contains(k)) {
            return false;
        }
        return rule.biomes().isEmpty() || rule.biomes().contains(k);
    }

    /** A random walk of up to {@code size} blocks, replacing only what the rule allows. */
    private static void vein(LimitedRegion region, Random random, Rule rule, int x, int y, int z, int min, int max) {
        int placed = 0;
        for (int step = 0; step < rule.source().size() * 3 && placed < rule.source().size(); step++) {
            if (y >= min && y < max && region.isInRegion(x, y, z) && rule.replaces().contains(region.getType(x, y, z))) {
                region.setType(x, y, z, rule.ore());
                placed++;
            }
            int[] d = STEPS[random.nextInt(STEPS.length)];
            x += d[0];
            y += d[1];
            z += d[2];
        }
    }

    /** Height for one vein: "uniform" over [min, max], "triangle" peaking in the middle. */
    static int pickY(Random random, int min, int max, String distribution) {
        int span = max - min + 1;
        if ("triangle".equals(distribution)) {
            return min + (random.nextInt(span) + random.nextInt(span)) / 2;
        }
        return min + random.nextInt(span);
    }
}
