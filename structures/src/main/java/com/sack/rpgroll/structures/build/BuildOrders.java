package com.sack.rpgroll.structures.build;

import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.catalog.BuildOrder;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ordena los bloques de una obra según su {@link BuildOrder}; determinista (desempate total por y, z, x). */
public final class BuildOrders {

    /**
     * Bloques adosables por nombre de material (sufijo o nombre exacto). Es una lista explícita y no un
     * {@code Tag} porque los Tag necesitan servidor.
     */
    private static final List<String> ATTACHABLE_SUFFIXES = List.of(
            "_TORCH", "_BUTTON", "_CARPET", "_RAIL", "_SIGN", "_DOOR", "_BED", "_PRESSURE_PLATE", "_BANNER");
    private static final List<String> ATTACHABLE_NAMES = List.of(
            "TORCH", "LEVER", "RAIL", "REDSTONE_WIRE", "LADDER", "VINE", "REDSTONE_TORCH", "TRIPWIRE_HOOK", "SNOW");

    private static final Comparator<SourceBlock> BOTTOM_UP = Comparator
            .comparingInt(SourceBlock::y).thenComparingInt(SourceBlock::z).thenComparingInt(SourceBlock::x);

    private BuildOrders() {
    }

    /** Bloque con su clave cara ya calculada (decorar-ordenar-desdecorar): {@code group} y {@code dist}. */
    private record Keyed(SourceBlock b, int group, long dist) {
    }

    public static List<SourceBlock> order(List<SourceBlock> rotated, BuildOrder order, List<Material> custom) {
        // Dentro de cada capa (y), o de cada pasada, los bloques-entidad van al final.
        Comparator<SourceBlock> byTile = Comparator.comparingInt(s -> s.tile() != null ? 1 : 0);
        Comparator<SourceBlock> byY = Comparator.comparingInt(SourceBlock::y);
        // group (etapa o rango) y dist se calculan UNA vez por bloque, no en cada comparación.
        Comparator<Keyed> head = Comparator.comparingInt(Keyed::group);
        Comparator<SourceBlock> tail;
        List<Keyed> keyed = new ArrayList<>(rotated.size());
        switch (order) {
            case LAYERS -> {
                rotated.forEach(s -> keyed.add(new Keyed(s, 0, 0)));
                tail = byY.thenComparing(byTile).thenComparingInt(SourceBlock::z)
                        .thenComparingInt(s -> Math.floorMod(s.z(), 2) == 0 ? s.x() : -s.x());
            }
            case CENTER_OUT -> {
                Map<Integer, int[]> bounds = new HashMap<>(); // y -> {minX, maxX, minZ, maxZ}
                for (SourceBlock s : rotated) {
                    int[] b = bounds.computeIfAbsent(s.y(), k -> new int[]{s.x(), s.x(), s.z(), s.z()});
                    b[0] = Math.min(b[0], s.x());
                    b[1] = Math.max(b[1], s.x());
                    b[2] = Math.min(b[2], s.z());
                    b[3] = Math.max(b[3], s.z());
                }
                // Distancia al centro de la capa multiplicada por 2 (así el centro es entero).
                for (SourceBlock s : rotated) {
                    int[] b = bounds.get(s.y());
                    long dx = 2L * s.x() - (b[0] + b[1]);
                    long dz = 2L * s.z() - (b[2] + b[3]);
                    keyed.add(new Keyed(s, 0, dx * dx + dz * dz));
                }
                head = Comparator.comparing(Keyed::b, byY.thenComparing(byTile)).thenComparingLong(Keyed::dist);
                tail = BOTTOM_UP;
            }
            case STAGES -> {
                rotated.forEach(s -> keyed.add(new Keyed(s, stage(s), 0)));
                tail = byY.thenComparing(byTile).thenComparing(BOTTOM_UP);
            }
            case CUSTOM -> {
                Map<String, Integer> rank = new HashMap<>();
                for (Material m : custom) {
                    rank.putIfAbsent(m.name(), rank.size());
                }
                rotated.forEach(s -> keyed.add(new Keyed(s,
                        rank.getOrDefault(s.data().getMaterial().name(), rank.size()), 0)));
                tail = byY.thenComparing(byTile).thenComparing(BOTTOM_UP);
            }
            default -> {
                rotated.forEach(s -> keyed.add(new Keyed(s, 0, 0)));
                tail = byY.thenComparing(byTile).thenComparing(BOTTOM_UP);
            }
        }
        keyed.sort(head.thenComparing(Keyed::b, tail.thenComparing(BOTTOM_UP)));
        List<SourceBlock> out = new ArrayList<>(keyed.size());
        keyed.forEach(k -> out.add(k.b()));
        return out;
    }

    /** 0 = sólido de cubo entero, 1 = otros no adosables, 2 = adosables. */
    private static int stage(SourceBlock s) {
        String name = s.data().getMaterial().name();
        if (ATTACHABLE_NAMES.contains(name) || ATTACHABLE_SUFFIXES.stream().anyMatch(name::endsWith)) {
            return 2;
        }
        return s.data().isOccluding() ? 0 : 1;
    }
}
