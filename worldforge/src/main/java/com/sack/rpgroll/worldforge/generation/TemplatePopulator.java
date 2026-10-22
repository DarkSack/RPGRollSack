package com.sack.rpgroll.worldforge.generation;

import com.sack.rpgroll.worldforge.profile.TemplateRule;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Biome;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.bukkit.structure.Structure;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Places a profile's templates in chunks as Minecraft generates them, through the chunk's
 * {@link LimitedRegion}. A template only goes where its whole box, plus one block around it for
 * Minecraft's edge updates, is inside that region; a try that does not fit is skipped and counted.
 * Chunks that already exist are never touched.
 */
final class TemplatePopulator extends BlockPopulator {

    record Rule(TemplateRule source, Structure structure, Set<String> biomes, Set<String> excluded) {
    }

    /** Placed and skipped counts since the world loaded, and the last placements, for /wf world inspect. */
    record Stats(Map<String, long[]> counts, List<String> recent) {
    }

    static final int NO_FIT = Integer.MIN_VALUE;
    private static final int RECENT = 10;
    private static final StructureRotation[] ROTATIONS = {StructureRotation.NONE, StructureRotation.CLOCKWISE_90,
            StructureRotation.CLOCKWISE_180, StructureRotation.COUNTERCLOCKWISE_90};

    private final List<Rule> rules;
    private final Map<String, AtomicLong[]> counts = new LinkedHashMap<>();
    private final Deque<String> recent = new ConcurrentLinkedDeque<>();

    TemplatePopulator(List<Rule> rules) {
        this.rules = List.copyOf(rules);
        rules.forEach(r -> counts.put(r.source().id(), new AtomicLong[] {new AtomicLong(), new AtomicLong()}));
    }

    @Override
    public void populate(WorldInfo info, Random random, int chunkX, int chunkZ, LimitedRegion region) {
        int worldMin = info.getMinHeight(), worldMax = info.getMaxHeight();
        int buffer = region.getBuffer(), bx = chunkX << 4, bz = chunkZ << 4;
        for (Rule rule : rules) {
            TemplateRule t = rule.source();
            if (random.nextInt(t.rarity()) != 0) {
                continue;
            }
            StructureRotation rot = "none".equals(t.rotation()) ? StructureRotation.NONE : ROTATIONS[random.nextInt(4)];
            BlockVector size = rule.structure().getSize();
            int[] box = footprint(size.getBlockX(), size.getBlockZ(), rot);
            // One block of margin on each side: Minecraft updates the blocks touching the template's edge.
            int ox = origin(random, bx - buffer + 1 - box[0], bx + 15 + buffer - 1 - box[1], bx);
            int oz = origin(random, bz - buffer + 1 - box[2], bz + 15 + buffer - 1 - box[3], bz);
            int y = ox == NO_FIT || oz == NO_FIT ? NO_FIT
                    : height(t, random, region, ox + (box[0] + box[1]) / 2, oz + (box[2] + box[3]) / 2, size.getBlockY(), worldMin, worldMax);
            if (y == NO_FIT) {
                counts.get(t.id())[1].incrementAndGet();
                continue;
            }
            int cx = ox + (box[0] + box[1]) / 2, cz = oz + (box[2] + box[3]) / 2;
            if (!biomeAllowed(rule, region.getBiome(cx, y, cz))) {
                continue;
            }
            rule.structure().place(region, new BlockVector(ox, y, oz), false, rot, Mirror.NONE, -1, 1f, random);
            counts.get(t.id())[0].incrementAndGet();
            recent.addFirst(t.id() + " " + info.getName() + " " + ox + " " + y + " " + oz + " " + rot.name());
            while (recent.size() > RECENT) {
                recent.pollLast();
            }
        }
    }

    Stats stats() {
        Map<String, long[]> out = new LinkedHashMap<>();
        counts.forEach((id, c) -> out.put(id, new long[] {c[0].get(), c[1].get()}));
        return new Stats(out, new ArrayList<>(recent));
    }

    /** Bottom of the template, or {@link #NO_FIT}. */
    private static int height(TemplateRule t, Random random, LimitedRegion region, int x, int z, int sy, int worldMin, int worldMax) {
        int low = t.minY() == null ? worldMin : Math.max(t.minY(), worldMin);
        int high = t.maxY() == null ? worldMax - 1 : Math.min(t.maxY(), worldMax - 1);
        int y;
        if ("surface".equals(t.placement())) {
            int ground = ground(region, x, z, worldMin, worldMax);
            if (ground == NO_FIT || ground < low || ground > high) {
                return NO_FIT;
            }
            y = ground + 1 + t.yOffset();
        } else {
            int top = Math.min(high, worldMax - sy);
            if (top < low) {
                return NO_FIT;
            }
            y = low + random.nextInt(top - low + 1);
        }
        return y < worldMin || y + sy > worldMax ? NO_FIT : y;
    }

    /** The first solid block from the top, through plants, leaves and logs; NO_FIT over water, lava or the void. */
    static int ground(LimitedRegion region, int x, int z, int worldMin, int worldMax) {
        for (int y = worldMax - 1; y >= worldMin; y--) {
            Material m = region.getType(x, y, z);
            if (m == Material.WATER || m == Material.LAVA) {
                return NO_FIT;
            }
            if (m.isSolid() && !Tag.LEAVES.isTagged(m) && !Tag.LOGS.isTagged(m)) {
                return y;
            }
        }
        return NO_FIT;
    }

    /**
     * Where the box lands relative to the placement point, as {minX, maxX, minZ, maxZ}. Minecraft
     * rotates around that point: 90° clockwise takes (x, z) to (-z, x).
     */
    static int[] footprint(int sx, int sz, StructureRotation rot) {
        return switch (rot) {
            case NONE -> new int[] {0, sx - 1, 0, sz - 1};
            case CLOCKWISE_90 -> new int[] {-(sz - 1), 0, 0, sx - 1};
            case CLOCKWISE_180 -> new int[] {-(sx - 1), 0, -(sz - 1), 0};
            case COUNTERCLOCKWISE_90 -> new int[] {0, sz - 1, -(sx - 1), 0};
        };
    }

    /** A point in [min, max], inside the chunk starting at {@code chunkMin} when possible; NO_FIT if the range is empty. */
    static int origin(Random random, int min, int max, int chunkMin) {
        int a = Math.max(min, chunkMin), b = Math.min(max, chunkMin + 15);
        if (a > b) {
            a = min;
            b = max;
        }
        return a > b ? NO_FIT : a + random.nextInt(b - a + 1);
    }

    private static boolean biomeAllowed(Rule rule, Biome biome) {
        String k = biome.getKey().toString();
        return !rule.excluded().contains(k) && (rule.biomes().isEmpty() || rule.biomes().contains(k));
    }
}
