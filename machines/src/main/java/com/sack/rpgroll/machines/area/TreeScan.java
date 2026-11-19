package com.sack.rpgroll.machines.area;

import org.bukkit.Material;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Las cuentas de la Leñadora: qué troncos forman un árbol y qué brote le toca. */
public final class TreeScan {

    private TreeScan() {
    }

    /**
     * Los bloques conectados a {@code start} (por los 26 vecinos) que cumplen {@code isLog} y caen
     * {@code inside}, empezando por {@code start}, hasta {@code max}. Cada uno es {x, y, z}.
     */
    public static List<int[]> collect(int[] start, Predicate<int[]> isLog, Predicate<int[]> inside, int max) {
        List<int[]> found = new ArrayList<>();
        if (max <= 0 || !isLog.test(start) || !inside.test(start)) {
            return found;
        }
        Set<Long> seen = new HashSet<>();
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(pack(start));
        while (!queue.isEmpty() && found.size() < max) {
            int[] at = queue.poll();
            found.add(at);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int[] next = {at[0] + dx, at[1] + dy, at[2] + dz};
                        if (seen.add(pack(next)) && inside.test(next) && isLog.test(next)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return found;
    }

    static long pack(int[] p) {
        return ((long) (p[0] & 0x3FFFFFF) << 38) | ((long) (p[1] & 0xFFF) << 26) | (p[2] & 0x3FFFFFF);
    }

    /** El brote de un tronco ({@code OAK_LOG} → {@code OAK_SAPLING}), o null si no es de un árbol. */
    public static Material sapling(Material log) {
        return switch (log) {
            case MANGROVE_LOG, MANGROVE_WOOD -> Material.MANGROVE_PROPAGULE;
            case CRIMSON_STEM, CRIMSON_HYPHAE -> Material.CRIMSON_FUNGUS;
            case WARPED_STEM, WARPED_HYPHAE -> Material.WARPED_FUNGUS;
            default -> {
                String name = log.name();
                if (name.startsWith("STRIPPED_") || !(name.endsWith("_LOG") || name.endsWith("_WOOD"))) {
                    yield null;
                }
                yield Material.getMaterial(name.substring(0, name.lastIndexOf('_')) + "_SAPLING");
            }
        };
    }
}
