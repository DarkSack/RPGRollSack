package com.sack.rpgroll.worldforge.protection;

import com.sack.rpgroll.worldforge.api.Ids;
import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.regen.RegionFiles.ChunkPos;
import com.sack.rpgroll.worldforge.storage.SafeFiles;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Areas that regeneration never touches ({@code protections.yml}), plus a square of chunks
 * around each world's spawn. If the file cannot be read, regeneration is blocked until it
 * is fixed: an unreadable protection list must not mean "nothing is protected".
 */
public final class Protections {

    public record Area(String id, String world, int minX, int minZ, int maxX, int maxZ, String note) {
        public boolean covers(String w, ChunkPos c) {
            int x0 = c.x() << 4, z0 = c.z() << 4;
            return world.equals(w) && x0 <= maxX && x0 + 15 >= minX && z0 <= maxZ && z0 + 15 >= minZ;
        }
    }

    static final int LIMIT = 30_000_000;
    private static final List<String> HEADER = List.of(
            "Zonas que la regeneración de WorldForge nunca toca: coordenadas de bloque, toda la altura.",
            "Edítalas con /wf protection add|remove, o a mano y luego /wf reload-config.",
            "Si este archivo tiene un error, la regeneración queda BLOQUEADA hasta corregirlo.",
            "spawn-radius-chunks: chunks protegidos alrededor de la aparición de cada mundo (0 = ninguno).");

    private final Path file;
    private List<Area> areas = List.of();
    private int spawnRadius = 2;
    private String broken;

    public Protections(Path file) {
        this.file = file;
    }

    /** Reads the file (creating it if missing). Returns what is wrong with it; empty if fine. */
    public synchronized List<Problem> load() {
        try {
            if (!Files.exists(file)) {
                areas = List.of();
                spawnRadius = 2;
                save();
            }
            YamlConfiguration doc = new YamlConfiguration();
            doc.loadFromString(Files.readString(file));
            List<Problem> problems = new ArrayList<>();
            int radius = doc.getInt("spawn-radius-chunks", 2);
            if (!doc.isInt("spawn-radius-chunks") || radius < 0 || radius > 32) {
                problems.add(Problem.error("spawn-radius-chunks", "debe ser un entero de 0 a 32."));
            }
            List<Area> read = new ArrayList<>();
            List<Map<?, ?>> list = doc.getMapList("areas");
            if (doc.contains("areas") && !doc.isList("areas")) {
                problems.add(Problem.error("areas", "debe ser una lista."));
            }
            for (int i = 0; i < list.size(); i++) {
                Map<?, ?> m = list.get(i);
                String at = "areas[" + i + "]";
                try {
                    Map<?, ?> from = (Map<?, ?>) m.get("from"), to = (Map<?, ?>) m.get("to");
                    Area a = area(String.valueOf(m.get("id")), String.valueOf(m.get("world")), coord(from, "x"),
                            coord(from, "z"), coord(to, "x"), coord(to, "z"), m.get("note") == null ? "" : String.valueOf(m.get("note")));
                    check(a, read).ifPresent(p -> problems.add(Problem.error(at + "." + p.field(), p.message())));
                    read.add(a);
                } catch (RuntimeException e) {
                    problems.add(Problem.error(at, "necesita id, world, from {x, z} y to {x, z} con números enteros."));
                }
            }
            if (problems.isEmpty()) {
                areas = List.copyOf(read);
                spawnRadius = radius;
                broken = null;
            } else {
                broken = problems.getFirst().field() + ": " + problems.getFirst().message();
            }
            return problems;
        } catch (IOException | InvalidConfigurationException e) {
            broken = "no se pudo leer: " + e.getMessage().lines().findFirst().orElse("");
            return List.of(Problem.error("", broken));
        }
    }

    /** Why regeneration is blocked right now, if it is. */
    public synchronized Optional<String> blockedReason() {
        return Optional.ofNullable(broken).map(b -> "protections.yml tiene un error (" + b + "); corrígelo y usa /wf reload-config.");
    }

    public synchronized List<Area> areas() {
        return areas;
    }

    public synchronized int spawnRadius() {
        return spawnRadius;
    }

    /** {@code spawn} may be null when the world is not loaded; the plan already excluded its spawn. */
    public synchronized boolean isProtected(String world, ChunkPos c, ChunkPos spawn) {
        if (spawn != null && Math.abs(c.x() - spawn.x()) <= spawnRadius && Math.abs(c.z() - spawn.z()) <= spawnRadius) {
            return true;
        }
        return areas.stream().anyMatch(a -> a.covers(world, c));
    }

    public synchronized Result<Area> add(String id, String world, int x1, int z1, int x2, int z2, String note) {
        if (broken != null) {
            return Result.fail("protections.yml", blockedReason().orElseThrow());
        }
        Area a = area(id, world, x1, z1, x2, z2, note);
        Optional<Problem> bad = check(a, areas);
        if (bad.isPresent()) {
            return Result.fail(List.of(bad.get()));
        }
        List<Area> before = areas;
        List<Area> next = new ArrayList<>(areas);
        next.add(a);
        areas = List.copyOf(next);
        return persist(before, a);
    }

    public synchronized Result<Area> remove(String id) {
        if (broken != null) {
            return Result.fail("protections.yml", blockedReason().orElseThrow());
        }
        Optional<Area> a = areas.stream().filter(x -> x.id().equals(id)).findFirst();
        if (a.isEmpty()) {
            return Result.fail("id", "no hay una zona protegida «" + id + "».");
        }
        List<Area> before = areas;
        areas = areas.stream().filter(x -> !x.id().equals(id)).toList();
        return persist(before, a.get());
    }

    private Result<Area> persist(List<Area> before, Area a) {
        try {
            save();
            return Result.ok(a);
        } catch (IOException e) {
            areas = before;
            return Result.fail("protections.yml", "no se pudo guardar: " + e.getMessage());
        }
    }

    private static Area area(String id, String world, int x1, int z1, int x2, int z2, String note) {
        return new Area(id, world, Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2), note);
    }

    private static Optional<Problem> check(Area a, List<Area> existing) {
        Optional<Problem> p = Ids.checkId("id", a.id()).or(() -> Ids.checkWorldName("world", a.world()));
        if (p.isPresent()) {
            return p;
        }
        if (Math.abs((long) a.minX()) > LIMIT || Math.abs((long) a.maxX()) > LIMIT
                || Math.abs((long) a.minZ()) > LIMIT || Math.abs((long) a.maxZ()) > LIMIT) {
            return Optional.of(Problem.error("from", "coordenadas fuera del mundo (±" + LIMIT + ")."));
        }
        if (a.note().length() > 128) {
            return Optional.of(Problem.error("note", "máximo 128 caracteres."));
        }
        if (existing.stream().anyMatch(x -> x.id().equals(a.id()))) {
            return Optional.of(Problem.error("id", "ya hay una zona «" + a.id() + "»."));
        }
        return Optional.empty();
    }

    private static int coord(Map<?, ?> m, String key) {
        Object v = m.get(key);
        if (v instanceof Integer i) {
            return i;
        }
        throw new IllegalArgumentException(key);
    }

    private static Map<String, Object> xz(int x, int z) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", x);
        m.put("z", z);
        return m;
    }

    private void save() throws IOException {
        YamlConfiguration doc = new YamlConfiguration();
        doc.options().setHeader(HEADER);
        doc.set("schema", 1);
        doc.set("spawn-radius-chunks", spawnRadius);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Area a : areas) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.id());
            m.put("world", a.world());
            m.put("from", xz(a.minX(), a.minZ()));
            m.put("to", xz(a.maxX(), a.maxZ()));
            if (!a.note().isEmpty()) {
                m.put("note", a.note());
            }
            list.add(m);
        }
        doc.set("areas", list);
        SafeFiles.writeAtomic(file, doc.saveToString());
    }
}
