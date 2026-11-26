package com.sack.rpgroll.structures.project;

import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.build.BlockWriter;
import com.sack.rpgroll.structures.build.PlacementPlan;
import com.sack.rpgroll.structures.catalog.MaterialSpec;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.storage.Box;
import com.sack.rpgroll.structures.storage.BuildRecord;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Cuentas de una reparación: qué falta en el mundo y cuánto cuesta ponerlo. */
public final class RepairMath {

    /** {@code missing} de {@code total} posiciones del plan faltan. */
    public record Result(int missing, int total) {
    }

    private RepairMath() {
    }

    /** {@link #of(PlacementPlan, BlockWriter, String, int, int, int, Set)} sin lista {@code replace}. */
    public static Result of(PlacementPlan plan, BlockWriter w, String world, int ox, int oy, int oz) {
        return of(plan, w, world, ox, oy, oz, Set.of());
    }

    /** Todo de una vez (pruebas y cajas pequeñas); en el servidor, {@link RepairScans} lo reparte en ticks. */
    public static Result of(PlacementPlan plan, BlockWriter w, String world, int ox, int oy, int oz,
                            Set<Material> replace) {
        return new Scan(plan, w, world, ox, oy, oz, replace).step(Integer.MAX_VALUE);
    }

    /**
     * Cuenta lo que falta, por tandas, en el hilo principal. Falta una posición del plan donde el plan pone otra cosa y
     * en el mundo hay aire, algo reemplazable o algo de {@code replace} (lo mismo que el motor pisa); lo que puso otro
     * no falta. Un chunk sin cargar termina con {@link ProjectService.RepairException} {@code repair.not-loaded}: leerlo
     * lo cargaría.
     */
    public static final class Scan {
        private final PlacementPlan plan;
        private final BlockWriter w;
        private final String world;
        private final int ox, oy, oz;
        private final Set<Material> replace;
        private int next, missing;

        public Scan(PlacementPlan plan, BlockWriter w, String world, int ox, int oy, int oz, Set<Material> replace) {
            this.plan = plan;
            this.w = w;
            this.world = world;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            this.replace = replace;
        }

        /** Mira hasta {@code n} posiciones más; el resultado al acabar, null mientras tanto. */
        public Result step(int n) {
            int end = (int) Math.min(plan.size(), (long) next + n);
            for (; next < end; next++) {
                SourceBlock b = plan.at(next);
                int x = ox + b.x(), z = oz + b.z();
                if (!w.chunkLoaded(world, x, z)) {
                    throw new ProjectService.RepairException(ProjectService.RepairException.NOT_LOADED);
                }
                BlockWriter.BlockInfo now = w.read(world, x, oy + b.y(), z);
                if (!b.data().equals(now.data())
                        && (isAir(now.type()) || now.replaceable() || replace.contains(now.type()))) {
                    missing++;
                }
            }
            return next >= plan.size() ? new Result(missing, plan.size()) : null;
        }
    }

    /** Por material (las specs con la misma clave se suman): {@code ceil(cantidad × missing / total)}. */
    public static Map<String, Integer> price(List<MaterialSpec> specs, int missing, int total) {
        Map<String, Integer> amounts = new LinkedHashMap<>();
        specs.forEach(s -> amounts.merge(s.key(), s.amount(), Integer::sum));
        Map<String, Integer> out = new LinkedHashMap<>();
        amounts.forEach((key, n) -> out.put(key, (int) ceil(n, missing, total)));
        return out;
    }

    /** Dinero de la reparación en céntimos: {@code ceil(cents × missing / total)}. */
    public static long money(long cents, int missing, int total) {
        return ceil(cents, missing, total);
    }

    /** ¿Está cargado cada chunk de la caja? */
    public static boolean loaded(BlockWriter w, String world, Box box) {
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                if (!w.chunkLoaded(world, cx << 4, cz << 4)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * ¿Se puede reparar {@code b} con la estructura de ahora? Mismo hash del esquemático (sin hash en uno de los dos:
     * sin comprobar, como las construcciones de antes de la v6) y la misma caja con su giro.
     */
    public static boolean matches(StructureDefinition def, BuildRecord b) {
        if (b.sourceHash() != null && def.sourceHash() != null && !b.sourceHash().equals(def.sourceHash())) {
            return false;
        }
        int w = b.rotation().rotatedWidth(def.blocks().width(), def.blocks().length());
        int l = b.rotation().rotatedLength(def.blocks().width(), def.blocks().length());
        return new Box(b.x(), b.y(), b.z(), b.x() + w - 1, b.y() + def.blocks().height() - 1, b.z() + l - 1)
                .equals(b.box());
    }

    private static long ceil(long n, int missing, int total) {
        return total <= 0 ? 0 : (n * missing + total - 1) / total;
    }

    /** {@code Material.isAir()} pide el registro del servidor; así sirve también en las pruebas. */
    private static boolean isAir(Material m) {
        return m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR;
    }
}
