package com.sack.rpgroll.worldforge.profile;

import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.api.Result;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Profile ↔ YAML. Reading collects every problem it finds (wrong type, missing key,
 * unknown key) instead of stopping at the first; writing is deterministic, so the same
 * profile always gives the same bytes and the same {@link #hash}.
 *
 * <p>Only the <em>shape</em> is checked here. Whether a block or biome exists, and whether
 * the provider supports a field, is {@link ProfileValidator}'s job.
 */
public final class ProfileCodec {

    private static final Set<String> TOP_KEYS = Set.of("schema", "id", "name", "description", "minecraft",
            "provider", "seed", "environment", "world-type", "structures", "spawn", "platform", "layers", "biomes",
            "ores", "templates", "player-gravity", "generator", "terrain");
    private static final Set<String> ORE_KEYS = Set.of("id", "block", "replaces", "veins-per-chunk", "size",
            "min-y", "max-y", "distribution", "biomes", "exclude-biomes", "priority", "enabled");
    private static final Set<String> TEMPLATE_KEYS = Set.of("id", "structure", "rarity", "placement", "min-y",
            "max-y", "y-offset", "rotation", "biomes", "exclude-biomes", "enabled");

    private ProfileCodec() {
    }

    public static Result<Profile> read(String yaml) {
        YamlConfiguration doc = new YamlConfiguration();
        try {
            doc.loadFromString(yaml);
        } catch (InvalidConfigurationException e) {
            return Result.fail("", "YAML mal formado: " + firstLine(e.getMessage()));
        }
        return read(toMap(doc));
    }

    static Result<Profile> read(Map<String, Object> root) {
        List<Problem> problems = new ArrayList<>();
        Node n = new Node("", root, problems);

        for (String key : root.keySet()) {
            if (!TOP_KEYS.contains(key)) {
                problems.add(Problem.warning(key, "clave desconocida, se ignora (¿una errata?)."));
            }
        }

        Integer schema = n.integer("schema", null);
        if (schema == null) {
            problems.add(Problem.error("schema", "falta: indica la versión del esquema (" + Profile.SCHEMA + ")."));
        } else if (schema > Profile.SCHEMA) {
            problems.add(Problem.error("schema", "versión " + schema + " es de un WorldForge más nuevo; este entiende hasta la " + Profile.SCHEMA + "."));
        } else if (schema < 1) {
            problems.add(Problem.error("schema", "versión " + schema + " no existe."));
        }

        String id = n.string("id", null);
        if (id == null) {
            problems.add(Problem.error("id", "falta."));
        }
        String provider = n.string("provider", null);
        if (provider == null) {
            problems.add(Problem.error("provider", "falta: vanilla, empty, flat, hybrid, terrain o plugin."));
        }
        String minecraft = n.string("minecraft", Profile.TARGET_MINECRAFT);
        if (!Profile.TARGET_MINECRAFT.equals(minecraft)) {
            problems.add(Problem.warning("minecraft", "el perfil es para " + minecraft + " y este servidor usa las reglas de " + Profile.TARGET_MINECRAFT + "."));
        }

        Profile.Spawn spawn = null;
        Node s = n.child("spawn");
        if (s != null) {
            Integer x = s.integer("x", null), y = s.integer("y", null), z = s.integer("z", null);
            if (x == null || y == null || z == null) {
                problems.add(Problem.error("spawn", "necesita x, y y z."));
            } else {
                spawn = new Profile.Spawn(x, y, z);
            }
        }

        Profile.Platform platform = null;
        Node p = n.child("platform");
        if (p != null) {
            String block = p.string("block", null);
            Integer radius = p.integer("radius", null), y = p.integer("y", null);
            if (block == null || radius == null || y == null) {
                problems.add(Problem.error("platform", "necesita block, radius e y."));
            } else {
                platform = new Profile.Platform(normalizeKey(block), radius, y);
            }
        }

        List<Profile.Layer> layers = new ArrayList<>();
        List<Node> layerNodes = n.list("layers");
        for (int i = 0; i < layerNodes.size(); i++) {
            Node l = layerNodes.get(i);
            String block = l.string("block", null);
            Integer height = l.integer("height", null);
            if (block == null || height == null) {
                problems.add(Problem.error("layers[" + i + "]", "necesita block y height."));
            } else {
                layers.add(new Profile.Layer(normalizeKey(block), height));
            }
        }

        List<OreRule> ores = new ArrayList<>();
        List<Node> oreNodes = n.list("ores");
        for (int i = 0; i < oreNodes.size(); i++) {
            OreRule rule = readOre(oreNodes.get(i), "ores[" + i + "]", problems);
            if (rule != null) {
                ores.add(rule);
            }
        }

        List<TemplateRule> templates = new ArrayList<>();
        List<Node> templateNodes = n.list("templates");
        for (int i = 0; i < templateNodes.size(); i++) {
            TemplateRule rule = readTemplate(templateNodes.get(i), "templates[" + i + "]", problems);
            if (rule != null) {
                templates.add(rule);
            }
        }

        Long seed = n.longValue("seed");
        Profile profile = new Profile(
                schema == null ? Profile.SCHEMA : schema,
                id,
                n.string("name", id),
                n.string("description", ""),
                minecraft,
                provider == null ? null : provider.toLowerCase(Locale.ROOT),
                seed,
                n.string("environment", "normal").toLowerCase(Locale.ROOT),
                n.string("world-type", "normal").toLowerCase(Locale.ROOT),
                n.boolOrNull("structures"),
                spawn,
                platform,
                layers,
                n.strings("biomes").stream().map(ProfileCodec::normalizeKey).toList(),
                ores,
                templates,
                n.number("player-gravity"),
                n.string("generator", null),
                n.string("terrain", null));
        return Result.of(profile, problems);
    }

    private static OreRule readOre(Node o, String path, List<Problem> problems) {
        for (String key : o.keys()) {
            if (!ORE_KEYS.contains(key)) {
                problems.add(Problem.warning(path + "." + key, "clave desconocida, se ignora."));
            }
        }
        String id = o.string("id", null);
        String block = o.string("block", null);
        Integer veins = o.integer("veins-per-chunk", null);
        Integer size = o.integer("size", null);
        Integer minY = o.integer("min-y", null);
        Integer maxY = o.integer("max-y", null);
        if (id == null || block == null || veins == null || size == null || minY == null || maxY == null) {
            problems.add(Problem.error(path, "necesita id, block, veins-per-chunk, size, min-y y max-y."));
            return null;
        }
        List<String> replaces = o.strings("replaces").stream().map(ProfileCodec::normalizeKey).toList();
        return new OreRule(id, normalizeKey(block), replaces.isEmpty() ? OreRule.DEFAULT_REPLACES : replaces,
                veins, size, minY, maxY, o.string("distribution", "uniform").toLowerCase(Locale.ROOT),
                o.strings("biomes").stream().map(ProfileCodec::normalizeKey).toList(),
                o.strings("exclude-biomes").stream().map(ProfileCodec::normalizeKey).toList(),
                o.integer("priority", 0), o.bool("enabled", true));
    }

    private static TemplateRule readTemplate(Node t, String path, List<Problem> problems) {
        for (String key : t.keys()) {
            if (!TEMPLATE_KEYS.contains(key)) {
                problems.add(Problem.warning(path + "." + key, "clave desconocida, se ignora."));
            }
        }
        String id = t.string("id", null);
        String structure = t.string("structure", null);
        Integer rarity = t.integer("rarity", null);
        if (id == null || structure == null || rarity == null) {
            problems.add(Problem.error(path, "necesita id, structure y rarity."));
            return null;
        }
        String placement = t.string("placement", "surface").toLowerCase(Locale.ROOT);
        return new TemplateRule(id, structure.trim().toLowerCase(Locale.ROOT), rarity, placement,
                t.integer("min-y", null), t.integer("max-y", null), t.integer("y-offset", 0),
                t.string("rotation", "random").toLowerCase(Locale.ROOT),
                t.strings("biomes").stream().map(ProfileCodec::normalizeKey).toList(),
                t.strings("exclude-biomes").stream().map(ProfileCodec::normalizeKey).toList(),
                t.bool("enabled", true));
    }

    /** Fixed key order, no timestamps: identical profiles give identical text. */
    public static String write(Profile p) {
        YamlConfiguration doc = new YamlConfiguration();
        doc.set("schema", p.schema());
        doc.set("id", p.id());
        doc.set("name", p.name());
        doc.set("description", p.description());
        doc.set("minecraft", p.minecraft());
        doc.set("provider", p.provider());
        if (p.seed() != null) {
            doc.set("seed", p.seed());
        }
        doc.set("environment", p.environment());
        doc.set("world-type", p.worldType());
        if (p.structures() != null) {
            doc.set("structures", p.structures());
        }
        if (p.spawn() != null) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("x", p.spawn().x());
            s.put("y", p.spawn().y());
            s.put("z", p.spawn().z());
            doc.set("spawn", s);
        }
        if (p.platform() != null) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("block", p.platform().block());
            s.put("radius", p.platform().radius());
            s.put("y", p.platform().y());
            doc.set("platform", s);
        }
        if (!p.layers().isEmpty()) {
            doc.set("layers", p.layers().stream().map(l -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("block", l.block());
                m.put("height", l.height());
                return m;
            }).toList());
        }
        if (!p.biomes().isEmpty()) {
            doc.set("biomes", p.biomes());
        }
        if (!p.ores().isEmpty()) {
            doc.set("ores", p.ores().stream().map(o -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", o.id());
                m.put("block", o.block());
                m.put("replaces", o.replaces());
                m.put("veins-per-chunk", o.veinsPerChunk());
                m.put("size", o.size());
                m.put("min-y", o.minY());
                m.put("max-y", o.maxY());
                m.put("distribution", o.distribution());
                m.put("biomes", o.biomes());
                m.put("exclude-biomes", o.excludeBiomes());
                m.put("priority", o.priority());
                m.put("enabled", o.enabled());
                return m;
            }).toList());
        }
        if (!p.templates().isEmpty()) {
            doc.set("templates", p.templates().stream().map(t -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", t.id());
                m.put("structure", t.structure());
                m.put("rarity", t.rarity());
                m.put("placement", t.placement());
                if (t.minY() != null) {
                    m.put("min-y", t.minY());
                }
                if (t.maxY() != null) {
                    m.put("max-y", t.maxY());
                }
                m.put("y-offset", t.yOffset());
                m.put("rotation", t.rotation());
                m.put("biomes", t.biomes());
                m.put("exclude-biomes", t.excludeBiomes());
                m.put("enabled", t.enabled());
                return m;
            }).toList());
        }
        if (p.playerGravity() != null) {
            doc.set("player-gravity", p.playerGravity());
        }
        if (p.generator() != null) {
            doc.set("generator", p.generator());
        }
        if (p.terrain() != null) {
            doc.set("terrain", p.terrain());
        }
        return doc.saveToString();
    }

    /** SHA-256 of the canonical text: the profile's version. */
    public static String hash(Profile p) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(write(p).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** "Stone" → "minecraft:stone"; keeps other namespaces. */
    public static String normalizeKey(String raw) {
        String k = raw.trim().toLowerCase(Locale.ROOT);
        return k.contains(":") ? k : "minecraft:" + k;
    }

    private static String firstLine(String s) {
        if (s == null) {
            return "";
        }
        int nl = s.indexOf('\n');
        return nl < 0 ? s : s.substring(0, nl);
    }

    private static Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object v = section.get(key);
            out.put(key, v instanceof ConfigurationSection c ? toMap(c) : v);
        }
        return out;
    }

    /** Typed reads over a YAML map, recording a problem for every wrong type. */
    private record Node(String path, Map<String, Object> map, List<Problem> problems) {

        Set<String> keys() {
            return map.keySet();
        }

        private String at(String key) {
            return path.isEmpty() ? key : path + "." + key;
        }

        String string(String key, String def) {
            Object v = map.get(key);
            if (v == null) {
                return def;
            }
            if (v instanceof String || v instanceof Number || v instanceof Boolean) {
                return v.toString();
            }
            problems.add(Problem.error(at(key), "debe ser texto."));
            return def;
        }

        Integer integer(String key, Integer def) {
            Object v = map.get(key);
            if (v == null) {
                return def;
            }
            if (v instanceof Integer i) {
                return i;
            }
            if (v instanceof Long l && l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return l.intValue();
            }
            problems.add(Problem.error(at(key), "debe ser un número entero (es «" + v + "»)."));
            return def;
        }

        Long longValue(String key) {
            Object v = map.get(key);
            if (v == null) {
                return null;
            }
            if (v instanceof Integer || v instanceof Long) {
                return ((Number) v).longValue();
            }
            problems.add(Problem.error(at(key), "debe ser un número entero (es «" + v + "»)."));
            return null;
        }

        Double number(String key) {
            Object v = map.get(key);
            if (v == null) {
                return null;
            }
            if (v instanceof Number num) {
                return num.doubleValue();
            }
            problems.add(Problem.error(at(key), "debe ser un número (es «" + v + "»)."));
            return null;
        }

        boolean bool(String key, boolean def) {
            Object v = map.get(key);
            if (v == null) {
                return def;
            }
            if (v instanceof Boolean b) {
                return b;
            }
            problems.add(Problem.error(at(key), "debe ser true o false."));
            return def;
        }

        Boolean boolOrNull(String key) {
            return map.get(key) == null ? null : bool(key, false);
        }

        List<String> strings(String key) {
            Object v = map.get(key);
            if (v == null) {
                return List.of();
            }
            if (v instanceof List<?> list && list.stream().allMatch(e -> e instanceof String)) {
                return list.stream().map(Object::toString).toList();
            }
            problems.add(Problem.error(at(key), "debe ser una lista de textos."));
            return List.of();
        }

        @SuppressWarnings("unchecked")
        Node child(String key) {
            Object v = map.get(key);
            if (v == null) {
                return null;
            }
            if (v instanceof Map<?, ?> m) {
                return new Node(at(key), (Map<String, Object>) m, problems);
            }
            problems.add(Problem.error(at(key), "debe ser una sección con claves."));
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Node> list(String key) {
            Object v = map.get(key);
            if (v == null) {
                return List.of();
            }
            if (!(v instanceof List<?> list)) {
                problems.add(Problem.error(at(key), "debe ser una lista."));
                return List.of();
            }
            List<Node> out = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) instanceof Map<?, ?> m) {
                    out.add(new Node(at(key) + "[" + i + "]", (Map<String, Object>) m, problems));
                } else {
                    problems.add(Problem.error(at(key) + "[" + i + "]", "debe ser una sección con claves."));
                }
            }
            return out;
        }
    }
}
