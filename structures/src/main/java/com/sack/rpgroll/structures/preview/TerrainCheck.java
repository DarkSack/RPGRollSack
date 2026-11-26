package com.sack.rpgroll.structures.preview;

import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.catalog.PlacementRules;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Validación pura del terreno: el mundo entra por {@link BlockLookup}. */
public final class TerrainCheck {

    /** Como mucho se anotan tantos choques (basta para pintarlos); al llegar se deja de mirar. */
    static final int MAX_COLLISIONS = 64;

    public record BlockInfo(Material type, boolean replaceable, boolean liquid, boolean solid,
                           Supplier<String> biome) {
        // biome es perezoso: pedirlo cuesta, y solo se lee una vez.
    }

    @FunctionalInterface
    public interface BlockLookup {
        BlockInfo at(int x, int y, int z);
    }

    private TerrainCheck() {
    }

    /**
     * Orden de motivos (el primero que falla): fuera del mundo, mundo no permitido, bioma no permitido, solapa otra
     * obra, bloques ocupados, sin suelo, protegido.
     *
     * @param ox origen = esquina MÍNIMA de la caja girada (w, h, l ya girados); {@code blocks} ya girados
     * @param protection protección de la caja entera, una sola vez al final: null si se puede; si no, la clave del
     *                   motivo
     */
    public static CheckResult check(String world, int minHeight, int maxHeight, int ox, int oy, int oz, int w, int h,
                                    int l, List<SourceBlock> blocks, PlacementRules rules, BlockLookup lookup,
                                    List<Footprint> others, Supplier<String> protection) {
        return scan(world, minHeight, maxHeight, ox, oy, oz, w, h, l, blocks, rules, lookup, others, protection)
                .step(Integer.MAX_VALUE);
    }

    /** Lo mismo que {@link #check}, repartido en pasos: para validar una estructura grande en varios ticks. */
    public static Scan scan(String world, int minHeight, int maxHeight, int ox, int oy, int oz, int w, int h,
                            int l, List<SourceBlock> blocks, PlacementRules rules, BlockLookup lookup,
                            List<Footprint> others, Supplier<String> protection) {
        return new Scan(header(world, minHeight, maxHeight, ox, oy, oz, w, h, l, rules, lookup, others), ox, oy, oz,
                blocks, rules, lookup, protection);
    }

    /** Lo que no depende de cada bloque: altura, mundo, bioma y choque con otras cajas. null si todo bien. */
    private static CheckResult header(String world, int minHeight, int maxHeight, int ox, int oy, int oz, int w,
                                      int h, int l, PlacementRules rules, BlockLookup lookup, List<Footprint> others) {
        if (oy < minHeight || oy + h - 1 >= maxHeight) {
            return new CheckResult(List.of(), "preview.out-of-world");
        }
        if (!rules.worlds().isEmpty() && rules.worlds().stream().noneMatch(world::equalsIgnoreCase)) {
            return new CheckResult(List.of(), "preview.world-not-allowed");
        }
        // shortcut: el bioma se mira solo en la esquina mínima; si hay estructuras que cruzan biomas, mirar la caja.
        if (!rules.biomes().isEmpty()) {
            String biome = normalize(lookup.at(ox, oy, oz).biome().get());
            if (rules.biomes().stream().noneMatch(b -> normalize(b).equals(biome))) {
                return new CheckResult(List.of(), "preview.biome-not-allowed");
            }
        }
        Footprint box = Footprint.of(ox, oy, oz, w, h, l);
        for (Footprint o : others) {
            if (box.intersects(o)) {
                return new CheckResult(List.of(), "preview.overlaps-project");
            }
        }
        return null;
    }

    /** Validación en curso: choques bloque a bloque, después el suelo y por último la protección. */
    public static final class Scan {
        private final int ox, oy, oz;
        private final List<SourceBlock> blocks;
        private final PlacementRules rules;
        private final BlockLookup lookup;
        private final Supplier<String> protection;
        private final List<int[]> collisions = new ArrayList<>();
        /** Siguiente bloque a mirar en busca de choques y de suelo. */
        private int hit, ground;
        private CheckResult result;

        private Scan(CheckResult header, int ox, int oy, int oz, List<SourceBlock> blocks, PlacementRules rules,
                     BlockLookup lookup, Supplier<String> protection) {
            this.result = header;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            this.blocks = blocks;
            this.rules = rules;
            this.lookup = lookup;
            this.protection = protection;
        }

        /** Mira como mucho {@code budget} posiciones del mundo; null si aún no terminó. */
        public CheckResult step(int budget) {
            if (result != null) {
                return result;
            }
            for (; hit < blocks.size() && budget > 0; hit++, budget--) {
                SourceBlock b = blocks.get(hit);
                int x = ox + b.x(), y = oy + b.y(), z = oz + b.z();
                if (occupied(lookup.at(x, y, z), rules)) {
                    collisions.add(new int[] { x, y, z });
                    if (collisions.size() >= MAX_COLLISIONS) {
                        hit = blocks.size(); // basta para pintarlos
                        break;
                    }
                }
            }
            if (hit < blocks.size()) {
                return null;
            }
            if (!collisions.isEmpty()) {
                return result = new CheckResult(collisions, "preview.blocked");
            }
            if (rules.needsGround()) {
                for (; ground < blocks.size(); ground++) {
                    SourceBlock b = blocks.get(ground);
                    if (b.y() != 0) {
                        continue;
                    }
                    if (budget-- <= 0) {
                        return null;
                    }
                    if (!lookup.at(ox + b.x(), oy - 1, oz + b.z()).solid()) {
                        return result = new CheckResult(collisions, "preview.no-ground");
                    }
                }
            }
            return result = new CheckResult(collisions, protection.get());
        }
    }

    private static boolean occupied(BlockInfo i, PlacementRules rules) {
        if (i.liquid()) {
            return !rules.allowLiquids();
        }
        return !(isAir(i.type()) || i.replaceable() || rules.replace().contains(i.type()));
    }

    /** {@code Material.isAir()} pide el registro del servidor; así sirve también en las pruebas. */
    private static boolean isAir(Material m) {
        return m == Material.AIR || m == Material.CAVE_AIR || m == Material.VOID_AIR;
    }

    private static String normalize(String biome) {
        String s = biome.toLowerCase(Locale.ROOT);
        return s.startsWith("minecraft:") ? s.substring(10) : s;
    }
}
