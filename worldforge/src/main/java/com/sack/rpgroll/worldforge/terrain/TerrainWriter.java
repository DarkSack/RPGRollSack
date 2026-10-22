package com.sack.rpgroll.worldforge.terrain;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Turns a {@link TerrainSpec} into datapack files, starting from the server's own vanilla JSON
 * ({@code vanilla} gives {@code data/minecraft/<path>.json} as text). Every
 * {@code minecraft:overworld…/…} density function the noise router reaches is copied to
 * {@code worldforge:<terrain>/…} with its references rewritten, then:
 * <ul>
 *   <li>{@code offset}, {@code factor}, {@code jaggedness} are wrapped so the land is stretched
 *       {@code k} times around y=64 (surface y = 128 + 128·offset in vanilla);</li>
 *   <li>the continents, erosion and ridges noises (and temperature and vegetation in the router)
 *       get a smaller {@code xz_scale}, so their features are wider;</li>
 *   <li>the depth gradient is extended past y=320 so taller dimension types keep working, and the
 *       router's top slide and surface bound follow the type's top.</li>
 * </ul>
 * The copies keep vanilla's caves, aquifers, surface rule and biome placement.
 */
public final class TerrainWriter {

    private static final Pattern REF = Pattern.compile("minecraft:(overworld[a-z_]*/[a-z0-9_/]+)");
    private static final Gson GSON = new Gson();
    /** The depth gradient reaches past any build height, so the copies do not depend on the dimension type. */
    static final int GRADIENT_TOP = 2032;

    private TerrainWriter() {
    }

    /** Where a terrain's noise settings go for a dimension of the given type. */
    public static String settingsPath(String terrain, String type) {
        return "data/worldforge/worldgen/noise_settings/" + terrain + "/" + type + ".json";
    }

    public static String settingsId(String terrain, String type) {
        return "worldforge:" + terrain + "/" + type;
    }

    /**
     * All the files, datapack-relative path → JSON. The density functions only depend on the
     * terrain; the noise settings (at {@code settingsPath}) also on the height range.
     */
    public static Map<String, String> files(TerrainSpec t, int minY, int height, String settingsPath,
                                            Function<String, String> vanilla) {
        int top = minY + height;
        JsonObject settings = read(vanilla, "worldgen/noise_settings/" + t.base()).getAsJsonObject();
        JsonObject router = settings.getAsJsonObject("noise_router");

        Map<String, JsonElement> functions = new LinkedHashMap<>();
        Deque<JsonElement> todo = new ArrayDeque<>();
        todo.add(router);
        while (!todo.isEmpty()) {
            forEachRef(todo.pop(), path -> {
                if (!functions.containsKey(path)) {
                    JsonElement f = read(vanilla, "worldgen/density_function/" + path);
                    functions.put(path, f);
                    todo.add(f);
                }
            });
        }

        double k = t.heightScale();
        Map<String, String> out = new LinkedHashMap<>();
        for (var e : functions.entrySet()) {
            String name = e.getKey().substring(e.getKey().lastIndexOf('/') + 1);
            JsonElement f = rewrite(e.getValue(), t.id());
            f = switch (name) {
                case "continents" -> scaleNoise(f, t.continentSize());
                case "erosion" -> scaleNoise(f, t.erosionSize());
                case "ridges" -> scaleNoise(f, t.ridgeSize());
                // surface' = 64 + k·(surface − 64) + h  ⇔  offset' = k·(offset + 0.5) − 0.5 + h/128
                case "offset" -> cached(add(-0.5 + t.heightOffset() / 128.0, mul(k, add(0.5, unwrap(f)))));
                // k times taller land needs k times the room for 3D noise around the surface
                case "factor" -> cached(mul(1 / k, unwrap(f)));
                case "jaggedness" -> cached(mul(k * t.peaks(), unwrap(f)));
                default -> f;
            };
            extendDepthGradient(f);
            out.put("data/worldforge/worldgen/density_function/" + t.id() + "/" + e.getKey() + ".json", GSON.toJson(f));
        }

        JsonObject r = rewrite(router, t.id()).getAsJsonObject();
        r.add("temperature", scaleNoise(r.get("temperature"), t.biomeSize()));
        r.add("vegetation", scaleNoise(r.get("vegetation"), t.biomeSize()));
        extendDepthGradient(r);
        int shift = top - 320;
        forEachObject(r, o -> {
            if (isType(o, "y_clamped_gradient") && o.get("from_value").getAsDouble() == 1.0 && o.get("to_value").getAsDouble() == 0.0
                    && o.get("from_y").getAsInt() >= 0) {
                o.addProperty("from_y", o.get("from_y").getAsInt() + shift);
                o.addProperty("to_y", o.get("to_y").getAsInt() + shift);
            }
        });
        JsonObject psl = r.getAsJsonObject("preliminary_surface_level");
        psl.addProperty("lower_bound", minY);
        JsonObject bound = psl.getAsJsonObject("upper_bound");
        if (isType(bound, "clamp")) {
            bound.addProperty("max", (double) top);
        }
        settings.add("noise_router", r);
        JsonObject noise = settings.getAsJsonObject("noise");
        noise.addProperty("min_y", minY);
        noise.addProperty("height", height);
        settings.addProperty("sea_level", t.seaLevel());
        settings.addProperty("aquifers_enabled", t.aquifers());
        settings.addProperty("ore_veins_enabled", t.oreVeins());
        out.put(settingsPath, GSON.toJson(settings));
        return out;
    }

    private static JsonElement read(Function<String, String> vanilla, String path) {
        String text = vanilla.apply(path);
        if (text == null) {
            throw new IllegalStateException("el servidor no trae data/minecraft/" + path + ".json");
        }
        return JsonParser.parseString(text);
    }

    private static void forEachRef(JsonElement e, java.util.function.Consumer<String> f) {
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            var m = REF.matcher(e.getAsString());
            if (m.matches()) {
                f.accept(m.group(1));
            }
        } else if (e.isJsonArray()) {
            e.getAsJsonArray().forEach(c -> forEachRef(c, f));
        } else if (e.isJsonObject()) {
            e.getAsJsonObject().entrySet().forEach(c -> forEachRef(c.getValue(), f));
        }
    }

    /** A copy with every {@code minecraft:overworld…/x} reference pointing to {@code worldforge:<terrain>/overworld…/x}. */
    private static JsonElement rewrite(JsonElement e, String terrain) {
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            var m = REF.matcher(e.getAsString());
            return m.matches() ? new JsonPrimitive("worldforge:" + terrain + "/" + m.group(1)) : e;
        }
        if (e.isJsonArray()) {
            JsonArray a = new JsonArray();
            e.getAsJsonArray().forEach(c -> a.add(rewrite(c, terrain)));
            return a;
        }
        if (e.isJsonObject()) {
            JsonObject o = new JsonObject();
            e.getAsJsonObject().entrySet().forEach(c -> o.add(c.getKey(), rewrite(c.getValue(), terrain)));
            return o;
        }
        return e;
    }

    private static void forEachObject(JsonElement e, java.util.function.Consumer<JsonObject> f) {
        if (e.isJsonArray()) {
            e.getAsJsonArray().forEach(c -> forEachObject(c, f));
        } else if (e.isJsonObject()) {
            f.accept(e.getAsJsonObject());
            e.getAsJsonObject().entrySet().forEach(c -> forEachObject(c.getValue(), f));
        }
    }

    /** Vanilla's depth: 1.5 at y=−64 to −1.5 at y=320, clamped. Same line, reaching higher. */
    private static void extendDepthGradient(JsonElement e) {
        forEachObject(e, o -> {
            if (isType(o, "y_clamped_gradient") && o.get("from_y").getAsInt() == -64 && o.get("to_y").getAsInt() == 320
                    && o.get("from_value").getAsDouble() == 1.5 && o.get("to_value").getAsDouble() == -1.5) {
                o.addProperty("to_y", GRADIENT_TOP);
                o.addProperty("to_value", 1.5 - 3.0 * (GRADIENT_TOP + 64) / 384);
            }
        });
    }

    /** Divides {@code xz_scale} of every shifted noise inside: features {@code size} times wider. */
    private static JsonElement scaleNoise(JsonElement e, double size) {
        forEachObject(e, o -> {
            if (isType(o, "shifted_noise")) {
                o.addProperty("xz_scale", o.get("xz_scale").getAsDouble() / size);
            }
        });
        return e;
    }

    private static boolean isType(JsonObject o, String type) {
        JsonElement t = o.get("type");
        return t != null && t.isJsonPrimitive() && (t.getAsString().equals("minecraft:" + type) || t.getAsString().equals(type));
    }

    /** Vanilla's offset/factor/jaggedness are {@code flat_cache(cache_2d(x))}; this returns x. */
    private static JsonElement unwrap(JsonElement e) {
        while (e.isJsonObject() && (isType(e.getAsJsonObject(), "flat_cache") || isType(e.getAsJsonObject(), "cache_2d"))) {
            e = e.getAsJsonObject().get("argument");
        }
        return e;
    }

    private static JsonObject cached(JsonElement x) {
        return node("flat_cache", "argument", node("cache_2d", "argument", x));
    }

    private static JsonObject add(double a, JsonElement b) {
        JsonObject o = node("add", "argument1", new JsonPrimitive(a));
        o.add("argument2", b);
        return o;
    }

    private static JsonObject mul(double a, JsonElement b) {
        JsonObject o = node("mul", "argument1", new JsonPrimitive(a));
        o.add("argument2", b);
        return o;
    }

    private static JsonObject node(String type, String key, JsonElement value) {
        JsonObject o = new JsonObject();
        o.addProperty("type", "minecraft:" + type);
        o.add(key, value);
        return o;
    }
}
