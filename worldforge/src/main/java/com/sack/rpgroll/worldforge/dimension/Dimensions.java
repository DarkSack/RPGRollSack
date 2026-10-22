package com.sack.rpgroll.worldforge.dimension;

import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.platform.ServerPlatform;
import com.sack.rpgroll.worldforge.storage.Database;
import com.sack.rpgroll.worldforge.storage.Records;
import com.sack.rpgroll.worldforge.storage.SafeFiles;
import com.sack.rpgroll.worldforge.terrain.TerrainSpec;
import com.sack.rpgroll.worldforge.terrain.TerrainWriter;
import com.sack.rpgroll.worldforge.terrain.Terrains;

import org.bukkit.Bukkit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/**
 * New dimensions through the only mechanism Minecraft has for them: a datapack in the main
 * world, read at startup (FEASIBILITY P5, P6). WorldForge writes
 * {@code <world>/datapacks/worldforge/data/worldforge/dimension/<id>.json}; after the next
 * restart the server loads it as the Bukkit world {@code world_worldforge_<id>}. Nothing
 * happens before that restart, and the commands say so. Removing one only removes its
 * definition: the dimension's world folder stays on disk.
 */
public final class Dimensions {

    public static final String NAMESPACE = "worldforge";
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_]{0,23}");

    public static final String PENDING_RESTART = "PENDING_RESTART";
    public static final String READY = "READY";
    public static final String ERROR = "ERROR";
    public static final String PENDING_REMOVAL = "PENDING_REMOVAL";
    public static final String REMOVED = "REMOVED";

    /** A dimension type: vanilla ones by key, or WorldForge's own (written next to the dimension). */
    public record Type(String id, String key, int minY, int height, String description) {
    }

    /** A vanilla generator definition and the dimension types whose height it fits. */
    public record Generator(String id, String json, Set<String> types, String description) {
    }

    public record Dimension(String id, String type, String generator, String state, String message, long updatedAt,
                            String createdBy, boolean loaded) {
        public String worldName() {
            return "world_" + NAMESPACE + "_" + id;
        }
    }

    public static final Map<String, Type> TYPES = new LinkedHashMap<>();
    public static final Map<String, Generator> GENERATORS = new LinkedHashMap<>();

    static {
        TYPES.put("overworld", new Type("overworld", "minecraft:overworld", -64, 384, "como el mundo normal: cielo, día y noche, −64 a 319"));
        TYPES.put("caves", new Type("caves", "minecraft:overworld_caves", -64, 384, "como el normal pero con techo y sin cielo"));
        TYPES.put("tall", new Type("tall", NAMESPACE + ":tall", -64, 448, "como el normal pero más alto: −64 a 383"));
        TYPES.put("towering", new Type("towering", NAMESPACE + ":towering", -64, 640, "como el normal pero muy alto: −64 a 575"));
        TYPES.put("nether", new Type("nether", "minecraft:the_nether", 0, 256, "como el Nether: techo, sin día ni noche"));
        TYPES.put("end", new Type("end", "minecraft:the_end", 0, 256, "como el End"));

        Set<String> normal = Set.of("overworld", "caves", "tall");
        GENERATORS.put("void", new Generator("void",
                "{\"type\":\"minecraft:flat\",\"settings\":{\"biome\":\"minecraft:the_void\",\"features\":false,\"lakes\":false,"
                        + "\"layers\":[{\"block\":\"minecraft:air\",\"height\":1}],\"structure_overrides\":[]}}",
                Set.of("overworld", "caves", "tall", "towering", "nether", "end"), "vacío (bioma the_void)"));
        GENERATORS.put("flat", new Generator("flat",
                "{\"type\":\"minecraft:flat\",\"settings\":{\"biome\":\"minecraft:plains\",\"features\":false,\"lakes\":false,"
                        + "\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1},{\"block\":\"minecraft:dirt\",\"height\":2},"
                        + "{\"block\":\"minecraft:grass_block\",\"height\":1}],\"structure_overrides\":[]}}",
                Set.of("overworld", "caves", "tall", "towering", "nether", "end"), "plano: bedrock, tierra y hierba"));
        GENERATORS.put("overworld", new Generator("overworld",
                "{\"type\":\"minecraft:noise\",\"settings\":\"minecraft:overworld\",\"biome_source\":{\"type\":\"minecraft:multi_noise\",\"preset\":\"minecraft:overworld\"}}",
                normal, "terreno y biomas del mundo normal"));
        GENERATORS.put("amplified", new Generator("amplified",
                "{\"type\":\"minecraft:noise\",\"settings\":\"minecraft:amplified\",\"biome_source\":{\"type\":\"minecraft:multi_noise\",\"preset\":\"minecraft:overworld\"}}",
                normal, "terreno amplificado"));
        GENERATORS.put("nether", new Generator("nether",
                "{\"type\":\"minecraft:noise\",\"settings\":\"minecraft:nether\",\"biome_source\":{\"type\":\"minecraft:multi_noise\",\"preset\":\"minecraft:nether\"}}",
                Set.of("nether"), "terreno y biomas del Nether"));
        GENERATORS.put("end", new Generator("end",
                "{\"type\":\"minecraft:noise\",\"settings\":\"minecraft:end\",\"biome_source\":{\"type\":\"minecraft:the_end\"}}",
                Set.of("end"), "islas del End"));
    }

    /** {@code terrain:<id>} generators: a terrain from {@code terrains/}, on the overworld's noise. */
    public static final String TERRAIN = "terrain:";
    public static final Set<String> TERRAIN_TYPES = Set.of("overworld", "caves", "tall", "towering");
    static final String MAIN_SETTINGS = "data/minecraft/worldgen/noise_settings/overworld.json";

    private final ServerPlatform platform;
    private final Database db;
    private final Records records;
    private final java.util.function.Supplier<Path> levelFolder;
    private final Terrains terrains;
    private final Function<String, String> vanilla;

    /**
     * {@code levelFolder} gives the main world's folder ({@code world}), whose datapacks the
     * server reads. A supplier because at STARTUP no world is loaded yet.
     */
    public Dimensions(ServerPlatform platform, Database db, Records records, java.util.function.Supplier<Path> levelFolder,
                      Terrains terrains, Function<String, String> vanilla) {
        this.platform = platform;
        this.db = db;
        this.records = records;
        this.levelFolder = levelFolder;
        this.terrains = terrains;
        this.vanilla = vanilla;
    }

    public Path datapack() {
        return levelFolder.get().resolve("datapacks").resolve(NAMESPACE);
    }

    /** Checks an id/type/generator combination. Empty if it can be written. */
    public static Optional<Problem> check(String id, String type, String generator) {
        if (id == null || !ID.matcher(id).matches()) {
            return Optional.of(Problem.error("id", "«" + id + "» no vale: minúsculas, números o '_', hasta 24."));
        }
        if (!TYPES.containsKey(type)) {
            return Optional.of(Problem.error("type", "tipo «" + type + "» desconocido; hay: " + String.join(", ", TYPES.keySet()) + "."));
        }
        if (generator != null && generator.startsWith(TERRAIN)) {
            return TERRAIN_TYPES.contains(type) ? Optional.empty() : Optional.of(Problem.error("generator", "los terrenos usan el ruido "
                    + "del mundo normal; úsalos con: " + String.join(", ", TERRAIN_TYPES.stream().sorted().toList()) + "."));
        }
        Generator g = GENERATORS.get(generator);
        if (g == null) {
            return Optional.of(Problem.error("generator", "generador «" + generator + "» desconocido; hay: " + String.join(", ", GENERATORS.keySet()) + "."));
        }
        if (!g.types().contains(type)) {
            return Optional.of(Problem.error("generator", "«" + generator + "» no cabe en la altura del tipo «" + type
                    + "»; úsalo con: " + String.join(", ", g.types().stream().sorted().toList()) + "."));
        }
        return Optional.empty();
    }

    /** The dimension definition file content, as Minecraft reads it. */
    static String dimensionJson(String type, String generator) {
        String gen = generator.startsWith(TERRAIN)
                ? "{\"type\":\"minecraft:noise\",\"settings\":\"" + TerrainWriter.settingsId(generator.substring(TERRAIN.length()), type)
                        + "\",\"biome_source\":{\"type\":\"minecraft:multi_noise\",\"preset\":\"minecraft:overworld\"}}"
                : GENERATORS.get(generator).json();
        return "{\"type\":\"" + TYPES.get(type).key() + "\",\"generator\":" + gen + "}\n";
    }

    /** What a generator makes, for messages. */
    public String describe(String generator) {
        if (generator.startsWith(TERRAIN)) {
            String id = generator.substring(TERRAIN.length());
            return "terreno «" + id + "»" + terrains.get(id).map(t -> ": " + t.description()).orElse("");
        }
        Generator g = GENERATORS.get(generator);
        return g == null ? generator : g.description();
    }

    public Terrains terrains() {
        return terrains;
    }

    /** Whether the server jar's vanilla worldgen JSON, which terrains start from, can be read. */
    public boolean vanillaReadable() {
        return vanilla.apply("worldgen/noise_settings/overworld") != null;
    }

    public CompletableFuture<Result<Dimension>> create(String id, String type, String generator, String actor) {
        Optional<Problem> bad = check(id, type, generator);
        if (bad.isPresent()) {
            return CompletableFuture.completedFuture(Result.fail(List.of(bad.get())));
        }
        List<Problem> warnings = new ArrayList<>();
        if (generator.startsWith(TERRAIN)) {
            Optional<TerrainSpec> t = terrains.get(generator.substring(TERRAIN.length()));
            if (t.isEmpty()) {
                return CompletableFuture.completedFuture(Result.fail("generator", "no hay un terreno «" + generator.substring(TERRAIN.length())
                        + "» en terrains/; hay: " + String.join(", ", terrains.list().stream().map(TerrainSpec::id).toList()) + "."));
            }
            Type ty = TYPES.get(type);
            int cut = t.get().slideStart(ty.minY() + ty.height());
            if (t.get().estimatedPeak() > cut) {
                warnings.add(Problem.warning("type", "este terreno sube hasta y≈" + t.get().estimatedPeak() + " y el tipo «" + type
                        + "» aplana desde y=" + cut + ": las cimas saldrán cortadas. Para que quepan: towering."));
            }
        }
        return get(id).thenCompose(existing -> {
            if (existing.isPresent() && !existing.get().state().equals(REMOVED)) {
                return CompletableFuture.completedFuture(Result.<Dimension>fail("id", "ya existe la dimensión «" + id + "» ("
                        + existing.get().state() + ")."));
            }
            return platform.async(() -> {
                try {
                    write(id, type, generator);
                    return Result.ok(true);
                } catch (IOException e) {
                    return Result.<Boolean>fail("", "no se pudo escribir el datapack: " + e.getMessage());
                }
            }).thenCompose(w -> {
                if (!w.isOk()) {
                    return CompletableFuture.completedFuture(Result.<Dimension>fail(w.problems()));
                }
                String msg = "se carga en el próximo arranque como " + "world_" + NAMESPACE + "_" + id;
                return save(id, type, generator, PENDING_RESTART, msg, actor)
                        .thenCompose(v -> records.history(actor, "dimension.create", id, type + " " + generator))
                        .thenCompose(v -> get(id)).thenApply(d -> Result.ok(d.orElseThrow(), warnings));
            });
        });
    }

    /** Removes the definition only; the world folder is kept. Effective after a restart. */
    public CompletableFuture<Result<Dimension>> remove(String id, String actor) {
        return get(id).thenCompose(existing -> {
            if (existing.isEmpty() || existing.get().state().equals(REMOVED)) {
                return CompletableFuture.completedFuture(Result.<Dimension>fail("id", "no hay una dimensión «" + id + "» de WorldForge."));
            }
            Dimension d = existing.get();
            return platform.async(() -> {
                try {
                    Files.deleteIfExists(definition(id));
                    return Result.ok(true);
                } catch (IOException e) {
                    return Result.<Boolean>fail("", "no se pudo quitar la definición: " + e.getMessage());
                }
            }).thenCompose(w -> {
                if (!w.isOk()) {
                    return CompletableFuture.completedFuture(Result.<Dimension>fail(w.problems()));
                }
                String state = d.loaded() ? PENDING_REMOVAL : REMOVED;
                String msg = "definición quitada; " + (d.loaded() ? "deja de cargarse al reiniciar. " : "")
                        + "La carpeta world/dimensions/" + NAMESPACE + "/" + id + " se conserva.";
                return setState(id, state, msg)
                        .thenCompose(v -> records.history(actor, "dimension.remove", id, null))
                        .thenCompose(v -> get(id)).thenApply(x -> Result.ok(x.orElseThrow()));
            });
        });
    }

    /** After startup: what the restart actually did to each dimension. */
    public CompletableFuture<List<String>> reconcile() {
        return list().thenCompose(all -> {
            List<String> out = new ArrayList<>();
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (Dimension d : all) {
                String next = switch (d.state()) {
                    case PENDING_RESTART, ERROR -> d.loaded() ? READY : ERROR;
                    case READY -> d.loaded() ? READY : ERROR;
                    case PENDING_REMOVAL -> d.loaded() ? PENDING_REMOVAL : REMOVED;
                    default -> d.state();
                };
                if (next.equals(d.state())) {
                    continue;
                }
                String msg = switch (next) {
                    case READY -> "cargada como " + d.worldName();
                    case REMOVED -> "ya no se carga; su carpeta se conserva";
                    default -> "el servidor no la cargó: mira el registro del arranque (un JSON que Minecraft rechaza) y que "
                            + "el datapack \"file/" + NAMESPACE + "\" esté activo (/datapack list)";
                };
                out.add((next.equals(ERROR) ? "✘ " : "") + "Dimensión " + d.id() + ": " + next + " — " + msg);
                chain = chain.thenCompose(v -> setState(d.id(), next, msg));
            }
            return chain.thenApply(v -> out);
        });
    }

    public CompletableFuture<List<Dimension>> list() {
        return db.submit(c -> {
            List<String[]> rows = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, dimension_type, generator, state, message, updated_at, created_by FROM dimensions ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[] {rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                            String.valueOf(rs.getLong(6)), rs.getString(7)});
                }
            }
            return rows;
        }).thenCompose(rows -> platform.global(() -> rows.stream().map(r -> new Dimension(r[0], r[1], r[2], r[3], r[4],
                Long.parseLong(r[5]), r[6], Bukkit.getWorld("world_" + NAMESPACE + "_" + r[0]) != null)).toList()));
    }

    public CompletableFuture<Optional<Dimension>> get(String id) {
        return list().thenApply(l -> l.stream().filter(d -> d.id().equals(id)).findFirst());
    }

    private void write(String id, String type, String generator) throws IOException {
        Path datapack = datapack();
        if (generator.startsWith(TERRAIN)) {
            TerrainSpec t = terrains.get(generator.substring(TERRAIN.length())).orElseThrow(() -> new IOException("terreno desconocido"));
            Type ty = TYPES.get(type);
            writeTerrain(terrainFiles(t, ty.minY(), ty.height(), TerrainWriter.settingsPath(t.id(), type)));
        }
        packMeta();
        if (TYPES.get(type).key().startsWith(NAMESPACE + ":")) {
            Path t = datapack.resolve("data").resolve(NAMESPACE).resolve("dimension_type").resolve(type + ".json");
            if (!Files.exists(t)) {
                try (var in = Dimensions.class.getResourceAsStream("/dimension_type/" + type + ".json")) {
                    if (in == null) {
                        throw new IOException("falta dimension_type/" + type + ".json en el jar");
                    }
                    SafeFiles.writeAtomic(t, new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        }
        SafeFiles.writeAtomic(definition(id), dimensionJson(type, generator));
    }

    private void packMeta() throws IOException {
        Path mcmeta = datapack().resolve("pack.mcmeta");
        if (!Files.exists(mcmeta)) {
            // Data pack format of Minecraft 26.1.2 (FEASIBILITY §1); the same file loaded in P5.
            SafeFiles.writeAtomic(mcmeta, "{\"pack\":{\"description\":\"RPGRoll-WorldForge: dimensiones\",\"min_format\":[101,0],\"max_format\":[101,1]}}\n");
        }
    }

    private Map<String, String> terrainFiles(TerrainSpec t, int minY, int height, String settingsPath) throws IOException {
        try {
            return TerrainWriter.files(t, minY, height, settingsPath, vanilla);
        } catch (RuntimeException e) {
            throw new IOException("no se pudo preparar el terreno «" + t.id() + "»: " + e.getMessage(), e);
        }
    }

    /**
     * Writes a terrain's files. One already written with other values is never replaced: a
     * dimension may be using it, and its new chunks would stop matching the old ones.
     */
    private void writeTerrain(Map<String, String> files) throws IOException {
        Path datapack = datapack();
        for (var e : files.entrySet()) {
            Path p = datapack.resolve(e.getKey());
            if (!e.getKey().equals(MAIN_SETTINGS) && Files.exists(p) && !Files.readString(p).equals(e.getValue())) {
                throw new IOException("ese terreno ya está escrito en el datapack con otros valores (" + e.getKey() + ") y puede "
                        + "usarlo otra dimensión; guarda tus cambios con otro id en terrains/");
            }
        }
        packMeta();
        for (var e : files.entrySet()) {
            SafeFiles.writeAtomic(datapack.resolve(e.getKey()), e.getValue());
        }
    }

    /** The terrain the main overworld uses from the next start, or empty for vanilla's. */
    public Optional<String> mainTerrain() {
        try {
            Path p = datapack().resolve(MAIN_SETTINGS);
            if (!Files.exists(p)) {
                return Optional.empty();
            }
            var m = Pattern.compile("\"worldforge:([a-z0-9_]+)/").matcher(Files.readString(p));
            return Optional.of(m.find() ? m.group(1) : "?");
        } catch (IOException e) {
            return Optional.of("?");
        }
    }

    /**
     * Gives the main overworld a terrain, the way Terralith does it: by replacing the
     * {@code minecraft:overworld} noise settings. {@code "vanilla"} removes the replacement.
     * Effective after a restart, for chunks not generated yet; returns what to tell the admin.
     */
    public CompletableFuture<Result<List<String>>> setMain(String terrain, String actor) {
        Optional<TerrainSpec> t = terrain.equals("vanilla") ? Optional.empty() : terrains.get(terrain);
        if (!terrain.equals("vanilla") && t.isEmpty()) {
            return CompletableFuture.completedFuture(Result.fail("terrain", "no hay un terreno «" + terrain + "» en terrains/."));
        }
        return platform.async(() -> {
            try {
                List<String> notes = new ArrayList<>();
                if (t.isEmpty()) {
                    Files.deleteIfExists(datapack().resolve(MAIN_SETTINGS));
                } else {
                    Type main = TYPES.get("overworld");
                    writeTerrain(terrainFiles(t.get(), main.minY(), main.height(), MAIN_SETTINGS));
                    int cut = t.get().slideStart(main.minY() + main.height());
                    if (t.get().estimatedPeak() > cut) {
                        notes.add("⚠ este terreno sube hasta y≈" + t.get().estimatedPeak() + " y el mundo principal aplana desde y=" + cut
                                + ": las cimas saldrán cortadas (el principal no puede cambiar de altura).");
                    }
                }
                notes.addAll(otherOverworldPacks());
                return Result.ok(notes);
            } catch (IOException e) {
                return Result.<List<String>>fail("", e.getMessage());
            }
        }).thenCompose(r -> r.isOk()
                ? records.history(actor, "terrain.main", terrain, null).thenApply(v -> r)
                : CompletableFuture.completedFuture(r));
    }

    /** Other datapacks that also replace the overworld's noise settings: the last one loaded wins. */
    private List<String> otherOverworldPacks() {
        List<String> out = new ArrayList<>();
        Path packs = levelFolder.get().resolve("datapacks");
        try (var list = Files.list(packs)) {
            for (Path p : list.toList()) {
                String name = p.getFileName().toString();
                if (name.equals(NAMESPACE)) {
                    continue;
                }
                boolean has = false;
                if (Files.isDirectory(p)) {
                    has = Files.exists(p.resolve(MAIN_SETTINGS));
                } else if (name.endsWith(".zip")) {
                    try (ZipFile z = new ZipFile(p.toFile())) {
                        has = z.getEntry(MAIN_SETTINGS) != null;
                    } catch (IOException ignored) {
                        // not a readable zip: Minecraft will not load it either
                    }
                }
                if (has) {
                    out.add("⚠ el datapack «" + name + "» también cambia el terreno del mundo normal; gana el que se cargue el último "
                            + "(/datapack list). Con Terralith, quítalo o deja el mundo principal en vanilla.");
                }
            }
        } catch (IOException ignored) {
            // no datapacks folder: nothing else competes
        }
        return out;
    }

    private Path definition(String id) throws IOException {
        Path folder = datapack().resolve("data").resolve(NAMESPACE).resolve("dimension");
        return SafeFiles.child(folder, id + ".json").orElseThrow(() -> new IOException("id no válido"));
    }

    private CompletableFuture<Void> save(String id, String type, String generator, String state, String message, String actor) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO dimensions(id, profile_id, state, message, updated_at, dimension_type, generator, created_by)
                    VALUES (?, '', ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET state = excluded.state, message = excluded.message, updated_at = excluded.updated_at,
                      dimension_type = excluded.dimension_type, generator = excluded.generator, created_by = excluded.created_by""")) {
                ps.setString(1, id);
                ps.setString(2, state);
                ps.setString(3, message);
                ps.setLong(4, System.currentTimeMillis());
                ps.setString(5, type);
                ps.setString(6, generator);
                ps.setString(7, actor);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private CompletableFuture<Void> setState(String id, String state, String message) {
        return db.submit(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE dimensions SET state = ?, message = ?, updated_at = ? WHERE id = ?")) {
                ps.setString(1, state);
                ps.setString(2, message);
                ps.setLong(3, System.currentTimeMillis());
                ps.setString(4, id);
                ps.executeUpdate();
            }
            return null;
        });
    }
}
