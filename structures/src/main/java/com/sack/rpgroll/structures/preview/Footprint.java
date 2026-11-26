package com.sack.rpgroll.structures.preview;

import com.sack.rpgroll.structures.storage.Box;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Caja de bloques de una estructura ya girada, extremos incluidos. */
public record Footprint(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    /** Esquina mínima + medida (ya girada). */
    public static Footprint of(int ox, int oy, int oz, int w, int h, int l) {
        return new Footprint(ox, oy, oz, ox + w - 1, oy + h - 1, oz + l - 1);
    }

    public static Footprint of(Box b) {
        return new Footprint(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ());
    }

    public boolean intersects(Footprint o) {
        return minX <= o.maxX && maxX >= o.minX && minY <= o.maxY && maxY >= o.minY && minZ <= o.maxZ
                && maxZ >= o.minZ;
    }

    /** Posiciones {x, y, z} de los bloques de las 12 aristas, sin repetir. */
    public List<int[]> edges() {
        Set<List<Integer>> seen = new LinkedHashSet<>();
        int[] xs = { minX, maxX }, ys = { minY, maxY }, zs = { minZ, maxZ };
        for (int x = minX; x <= maxX; x++) {
            for (int y : ys) {
                for (int z : zs) {
                    seen.add(List.of(x, y, z));
                }
            }
        }
        for (int y = minY; y <= maxY; y++) {
            for (int x : xs) {
                for (int z : zs) {
                    seen.add(List.of(x, y, z));
                }
            }
        }
        for (int z = minZ; z <= maxZ; z++) {
            for (int x : xs) {
                for (int y : ys) {
                    seen.add(List.of(x, y, z));
                }
            }
        }
        List<int[]> out = new ArrayList<>(seen.size());
        for (List<Integer> p : seen) {
            out.add(new int[] { p.get(0), p.get(1), p.get(2) });
        }
        return out;
    }
}
