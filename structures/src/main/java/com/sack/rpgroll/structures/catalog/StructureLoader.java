package com.sack.rpgroll.structures.catalog;

import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.api.StructureModule;
import com.sack.rpgroll.structures.config.CategorySettings;
import com.sack.rpgroll.structures.config.WorkbenchSettings;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Lee {@code structures/<id>.yml} y valida; cada fallo es un {@link Problem} y esa estructura no entra. */
public final class StructureLoader {

    public record LoadResult(List<StructureDefinition> valid, List<Problem> problems) {}

    private static final Pattern ID = Pattern.compile("[a-z0-9_-]+");

    private StructureLoader() {
    }

    /**
     * {@code reader} envuelve {@code BlockSources.read}: puede lanzar {@link java.io.UncheckedIOException} (o cualquier
     * RuntimeException) y su mensaje pasa a ser el motivo.
     */
    public static LoadResult load(File structuresDir, File schematicsDir, CategorySettings cats,
                                  Map<WorkbenchTier, WorkbenchSettings.Size> maxSizes,
                                  Predicate<String> materialExists, Function<File, BlockSource> reader) {
        return load(structuresDir, schematicsDir, cats, maxSizes, materialExists, reader, type -> Optional.empty());
    }

    /** {@code modules}: el módulo de cada {@code module.type}; uno desconocido es un problema y la estructura no entra. */
    public static LoadResult load(File structuresDir, File schematicsDir, CategorySettings cats,
                                  Map<WorkbenchTier, WorkbenchSettings.Size> maxSizes,
                                  Predicate<String> materialExists, Function<File, BlockSource> reader,
                                  Function<String, Optional<StructureModule>> modules) {
        List<StructureDefinition> valid = new ArrayList<>();
        List<Problem> problems = new ArrayList<>();
        File[] files = structuresDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) {
            return new LoadResult(valid, problems);
        }
        Arrays.sort(files);
        for (File f : files) {
            List<Problem> mine = new ArrayList<>();
            StructureDefinition def = one(f, schematicsDir, cats, maxSizes, materialExists, reader, modules, mine);
            if (mine.isEmpty() && def != null) {
                valid.add(def);
            } else {
                problems.addAll(mine);
            }
        }
        // upgrade_from se mira con todo cargado; quitar una puede dejar sin base a otra que la mejora.
        boolean removed = true;
        while (removed) {
            Set<String> ids = new java.util.HashSet<>();
            valid.forEach(d -> ids.add(d.id()));
            removed = valid.removeIf(d -> {
                String from = d.upgradeFrom();
                if (from == null || (!from.equals(d.id()) && ids.contains(from))) {
                    return false;
                }
                problems.add(new Problem(d.id() + ".yml", "upgrade_from", from.equals(d.id())
                        ? "una estructura no se mejora a sí misma" : "estructura desconocida: " + from));
                return true;
            });
        }
        return new LoadResult(valid, problems);
    }

    private static StructureDefinition one(File f, File schematicsDir, CategorySettings cats,
                                           Map<WorkbenchTier, WorkbenchSettings.Size> maxSizes,
                                           Predicate<String> materialExists, Function<File, BlockSource> reader,
                                           Function<String, Optional<StructureModule>> modules,
                                           List<Problem> out) {
        String file = f.getName();
        String id = file.substring(0, file.length() - 4);
        if (!ID.matcher(id).matches()) {
            out.add(new Problem(file, "id", "id no válido: " + id));
        }
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.load(f);
        } catch (Exception e) {
            out.add(new Problem(file, "yaml", "YAML ilegible: " + e.getMessage()));
            return null;
        }

        Map<String, String> names = new LinkedHashMap<>();
        ConfigurationSection ns = y.getConfigurationSection("name");
        if (ns != null) {
            for (String k : ns.getKeys(false)) {
                names.put(k, ns.getString(k, ""));
            }
        }
        if (names.getOrDefault("es", "").isBlank()) {
            out.add(new Problem(file, "name.es", "falta el nombre en español"));
        }

        String category = y.getString("category", "").toLowerCase(Locale.ROOT);
        if (cats.byId(category).isEmpty()) {
            out.add(new Problem(file, "category", "categoría desconocida: " + category));
        }

        WorkbenchTier tier = WorkbenchTier.ofLevel(y.getInt("tier", 0)).orElse(null);
        if (tier == null) {
            out.add(new Problem(file, "tier", "nivel fuera de 1..3: " + y.get("tier")));
        }

        String schem = y.getString("schematic", "");
        File schematic = new File(schematicsDir, schem);
        BlockSource blocks = null;
        String hash = null;
        if (!schem.isBlank() && !inside(schematicsDir, schematic)) {
            out.add(new Problem(file, "schematic", "fuera de la carpeta schematics"));
        } else if (schem.isBlank() || !schematic.isFile()) {
            out.add(new Problem(file, "schematic", "no existe el archivo " + schem));
        } else {
            try {
                blocks = reader.apply(schematic);
                hash = contentHash(blocks);
            } catch (RuntimeException e) {
                out.add(new Problem(file, "schematic", "esquemático ilegible: " + e.getMessage()));
            }
        }
        if (blocks != null && tier != null) {
            var max = maxSizes.get(tier);
            var plain = new WorkbenchSettings.Size(blocks.width(), blocks.height(), blocks.length());
            var turned = new WorkbenchSettings.Size(blocks.length(), blocks.height(), blocks.width());
            if (max != null && !max.fits(plain) && !max.fits(turned)) {
                out.add(new Problem(file, "schematic", "más grande que el nivel " + tier.level() + " ("
                        + blocks.width() + "×" + blocks.height() + "×" + blocks.length() + ")"));
            }
        }

        List<MaterialSpec> materials = materials(y, file, materialExists, out);

        ConfigurationSection b = y.getConfigurationSection("build");
        boolean instant = b != null && "instant".equalsIgnoreCase(b.getString("mode", ""));
        BuildOrder order = BuildOrder.BOTTOM_UP;
        List<Material> custom = new ArrayList<>();
        int perCycle = 16;
        int ticks = 20;
        if (b != null) {
            String o = b.getString("order", "bottom_up");
            try {
                order = BuildOrder.valueOf(o.toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException e) {
                out.add(new Problem(file, "build.order", "orden desconocido: " + o));
            }
            for (String c : b.getStringList("custom")) {
                Material m = Material.matchMaterial(c);
                if (m == null) {
                    out.add(new Problem(file, "build.custom", "material desconocido: " + c));
                } else {
                    custom.add(m);
                }
            }
            perCycle = b.getInt("blocks-per-cycle", 16);
            ticks = b.getInt("ticks-per-cycle", 20);
        }
        if (order == BuildOrder.CUSTOM && custom.isEmpty()) {
            out.add(new Problem(file, "build.custom", "el orden custom necesita build.custom"));
        }
        if (perCycle <= 0) {
            out.add(new Problem(file, "build.blocks-per-cycle", "debe ser mayor que 0"));
        }
        if (ticks <= 0) {
            out.add(new Problem(file, "build.ticks-per-cycle", "debe ser mayor que 0"));
        }

        ConfigurationSection p = y.getConfigurationSection("placement");
        Set<Material> replace = new LinkedHashSet<>();
        if (p != null) {
            for (String r : p.getStringList("replace")) {
                Material m = Material.matchMaterial(r);
                if (m == null) {
                    out.add(new Problem(file, "placement.replace", "material desconocido: " + r));
                } else {
                    replace.add(m);
                }
            }
        }
        PlacementRules rules = new PlacementRules(p != null && p.getBoolean("ground", false),
                p != null && p.getBoolean("liquids", false),
                p == null ? List.of() : List.copyOf(p.getStringList("worlds")),
                p == null ? List.of() : List.copyOf(p.getStringList("biomes")), Set.copyOf(replace));

        ConfigurationSection ms = y.getConfigurationSection("module");
        String module = null;
        if (ms == null && y.contains("module")) {
            out.add(new Problem(file, "module", "debe ser una sección con type"));
        } else if (ms != null) {
            String type = ms.getString("type", "").toLowerCase(Locale.ROOT);
            Optional<StructureModule> m = modules.apply(type);
            if (m.isEmpty()) {
                out.add(new Problem(file, "module.type", "módulo desconocido: " + type));
            } else {
                module = type;
                try {
                    m.get().validate(ms, blocks).forEach(pr -> out.add(new Problem(file, "module." + pr.key(), pr.reason())));
                } catch (RuntimeException | LinkageError e) {
                    out.add(new Problem(file, "module", "el módulo falló al validar: " + e));
                }
            }
        }

        String perm = y.getString("permission", "");
        if (perm.isBlank()) {
            perm = "rpgrollstructures.build." + id;
        }
        String upgradeFrom = y.getString("upgrade_from", "").trim().toLowerCase(Locale.ROOT);
        if (!out.isEmpty()) {
            return null;
        }
        return new StructureDefinition(id, Map.copyOf(names), category, tier, y.getString("icon", ""), schematic,
                blocks, List.copyOf(materials), Math.max(0, y.getDouble("money", 0)), instant, order,
                List.copyOf(custom), perCycle, ticks, rules, perm, hash, module, ms,
                upgradeFrom.isEmpty() ? null : upgradeFrom);
    }

    /**
     * SHA-256 de los bloques ya leídos (medida y, en orden y-z-x, posición + {@code BlockData.getAsString()}), no de
     * los bytes del archivo: volver a guardar el mismo esquemático (otra cabecera gzip, otros metadatos) no lo
     * cambia. shortcut: el NBT de los bloques-entidad no entra; si hace falta, añadir su contenido aquí.
     */
    static String contentHash(BlockSource src) {
        List<SourceBlock> sorted = new ArrayList<>(src.blocks());
        sorted.sort(Comparator.comparingInt(SourceBlock::y).thenComparingInt(SourceBlock::z)
                .thenComparingInt(SourceBlock::x));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((src.width() + "x" + src.height() + "x" + src.length() + ";").getBytes(StandardCharsets.UTF_8));
            for (SourceBlock b : sorted) {
                String data = b.data() == null ? "?" : b.data().getAsString();
                md.update((b.x() + "," + b.y() + "," + b.z() + "=" + data + ";").getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@code f} queda dentro de {@code dir} una vez resueltos {@code ..} y enlaces. */
    private static boolean inside(File dir, File f) {
        try {
            return f.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator);
        } catch (IOException e) {
            return false;
        }
    }


    private static List<MaterialSpec> materials(YamlConfiguration y, String file, Predicate<String> exists,
                                                List<Problem> out) {
        List<MaterialSpec> list = new ArrayList<>();
        List<Map<?, ?>> raw = y.getMapList("materials");
        for (int i = 0; i < raw.size(); i++) {
            Map<?, ?> m = raw.get(i);
            String key = "materials[" + i + "]";
            List<String> ids = new ArrayList<>();
            if (m.get("any_of") instanceof List<?> l) {
                l.forEach(o -> ids.add(normalize(String.valueOf(o))));
            } else if (m.get("item") != null) {
                ids.add(normalize(String.valueOf(m.get("item"))));
            }
            if (ids.isEmpty()) {
                out.add(new Problem(file, key, "falta item o any_of"));
                continue;
            }
            for (String id : ids) {
                if (!exists.test(id)) {
                    out.add(new Problem(file, key, "material desconocido: " + id));
                }
            }
            int amount = m.get("amount") instanceof Number n ? n.intValue() : 0;
            if (amount <= 0) {
                out.add(new Problem(file, key, "cantidad no válida: " + m.get("amount")));
            }
            list.add(new MaterialSpec(List.copyOf(ids), amount, Boolean.TRUE.equals(m.get("optional"))));
        }
        return list;
    }

    private static String normalize(String id) {
        String s = id.trim().toLowerCase(Locale.ROOT);
        return s.contains(":") ? s : "minecraft:" + s;
    }
}
