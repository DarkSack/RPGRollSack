package com.sack.rpgroll.worldforge.terrain;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TerrainTest {

    private static final String PLAIN = "id: llano\n";

    private static TerrainSpec spec(String yaml) {
        var r = TerrainSpec.parse(yaml);
        assertTrue(r.isOk(), r.toString());
        return r.value();
    }

    @Test
    void built_in_terrains_parse_and_are_named_like_their_file() throws IOException {
        for (String id : Terrains.BUILT_IN) {
            try (InputStream in = TerrainTest.class.getResourceAsStream("/terrains/" + id + ".yml")) {
                assertNotNull(in, id);
                assertEquals(id, spec(new String(in.readAllBytes(), StandardCharsets.UTF_8)).id());
            }
        }
    }

    @Test
    void bad_terrain_files_are_refused_with_the_field() {
        assertEquals(1.0, spec(PLAIN).heightScale(), "defaults are vanilla's");
        assertEquals(63, spec(PLAIN).seaLevel());
        for (String[] bad : new String[][] {
                {"id: x\nheight-scale: 9\n", "height-scale"}, {"id: x\naltura: 2\n", "altura"}, {"id: X\n", "id"},
                {"id: x\nbase: nether\n", "base"}, {"id: x\npeaks: mucho\n", "peaks"}, {"id: x\naquifers: 3\n", "aquifers"},
                {"id: x\ncontinent-size: 0\n", "continent-size"}, {"height-scale: 1\n", "id"}}) {
            var r = TerrainSpec.parse(bad[0]);
            assertFalse(r.isOk(), bad[0]);
            assertEquals(bad[1], r.errors().getFirst().field(), bad[0]);
        }
    }

    @Test
    void the_peak_estimate_says_which_type_fits() {
        TerrainSpec vanilla = spec(PLAIN);
        assertEquals(240, vanilla.estimatedPeak());
        assertTrue(vanilla.estimatedPeak() <= vanilla.slideStart(320));
        TerrainSpec tall = spec("id: x\nheight-scale: 2.4\n");
        assertTrue(tall.estimatedPeak() > tall.slideStart(384) && tall.estimatedPeak() <= tall.slideStart(576));
    }

    /** A tiny router with the same shape as vanilla's, so the transform is checked without the server jar. */
    private static Function<String, String> fixture() {
        Map<String, String> m = new HashMap<>();
        m.put("worldgen/noise_settings/overworld", """
                {"noise":{"min_y":-64,"height":384,"size_horizontal":1,"size_vertical":2},"sea_level":63,
                 "aquifers_enabled":true,"ore_veins_enabled":true,"noise_router":{
                  "depth":"minecraft:overworld/depth","continents":"minecraft:overworld/continents",
                  "temperature":{"type":"minecraft:shifted_noise","noise":"minecraft:temperature","xz_scale":0.25,"y_scale":0.0},
                  "vegetation":{"type":"minecraft:shifted_noise","noise":"minecraft:vegetation","xz_scale":0.25,"y_scale":0.0},
                  "final_density":{"type":"minecraft:mul","argument1":{"type":"minecraft:y_clamped_gradient","from_y":240,"to_y":256,
                    "from_value":1.0,"to_value":0.0},"argument2":"minecraft:overworld/depth"},
                  "preliminary_surface_level":{"type":"minecraft:find_top_surface","cell_height":8,"lower_bound":-64,
                    "density":{"type":"minecraft:y_clamped_gradient","from_y":-64,"to_y":320,"from_value":1.5,"to_value":-1.5},
                    "upper_bound":{"type":"minecraft:clamp","input":"minecraft:overworld/offset","min":-40.0,"max":320.0}}}}""");
        m.put("worldgen/density_function/overworld/depth", """
                {"type":"minecraft:add","argument1":{"type":"minecraft:y_clamped_gradient","from_y":-64,"to_y":320,
                 "from_value":1.5,"to_value":-1.5},"argument2":"minecraft:overworld/offset"}""");
        m.put("worldgen/density_function/overworld/offset", """
                {"type":"minecraft:flat_cache","argument":{"type":"minecraft:cache_2d","argument":
                 {"type":"minecraft:mul","argument1":0.1,"argument2":"minecraft:overworld/continents"}}}""");
        m.put("worldgen/density_function/overworld/continents", """
                {"type":"minecraft:flat_cache","argument":{"type":"minecraft:shifted_noise","noise":"minecraft:continentalness",
                 "xz_scale":0.25,"y_scale":0.0}}""");
        return m::get;
    }

    private static JsonObject json(Map<String, String> files, String path) {
        String text = files.get(path);
        assertNotNull(text, path + " in " + files.keySet());
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void the_transform_on_a_small_router() {
        TerrainSpec t = spec("id: alto\nheight-scale: 2\nheight-offset: 16\ncontinent-size: 4\nbiome-size: 2\nsea-level: 40\naquifers: false\n");
        Map<String, String> files = TerrainWriter.files(t, -64, 640, TerrainWriter.settingsPath("alto", "towering"), fixture());
        String df = "data/worldforge/worldgen/density_function/alto/overworld/";
        assertEquals(java.util.Set.of(df + "depth.json", df + "offset.json", df + "continents.json",
                "data/worldforge/worldgen/noise_settings/alto/towering.json"), files.keySet());

        JsonObject s = json(files, "data/worldforge/worldgen/noise_settings/alto/towering.json");
        assertEquals(640, s.getAsJsonObject("noise").get("height").getAsInt());
        assertEquals(40, s.get("sea_level").getAsInt());
        assertFalse(s.get("aquifers_enabled").getAsBoolean());
        JsonObject r = s.getAsJsonObject("noise_router");
        assertEquals("worldforge:alto/overworld/depth", r.get("depth").getAsString());
        assertEquals(0.125, r.getAsJsonObject("temperature").get("xz_scale").getAsDouble());
        // top 576 instead of 320: the slide and the surface bound move 256 up
        JsonObject slide = r.getAsJsonObject("final_density").getAsJsonObject("argument1");
        assertEquals(496, slide.get("from_y").getAsInt());
        assertEquals(512, slide.get("to_y").getAsInt());
        assertEquals(576.0, r.getAsJsonObject("preliminary_surface_level").getAsJsonObject("upper_bound").get("max").getAsDouble());
        assertGradientExtended(r.getAsJsonObject("preliminary_surface_level").getAsJsonObject("density"));

        assertEquals(0.0625, json(files, df + "continents.json").getAsJsonObject("argument").get("xz_scale").getAsDouble());
        assertGradientExtended(json(files, df + "depth.json").getAsJsonObject("argument1"));
        // offset' = flat_cache(cache_2d(c + k·(0.5 + offset))) with c = −0.5 + 16/128
        JsonObject add = json(files, df + "offset.json").getAsJsonObject("argument").getAsJsonObject("argument");
        assertEquals(-0.375, add.get("argument1").getAsDouble());
        JsonObject mul = add.getAsJsonObject("argument2");
        assertEquals(2.0, mul.get("argument1").getAsDouble());
        JsonObject inner = mul.getAsJsonObject("argument2");
        assertEquals(0.5, inner.get("argument1").getAsDouble());
        assertEquals("minecraft:mul", inner.getAsJsonObject("argument2").get("type").getAsString(), "vanilla's cache markers are unwrapped once");
    }

    private static void assertGradientExtended(JsonObject g) {
        assertEquals(-64, g.get("from_y").getAsInt());
        assertEquals(TerrainWriter.GRADIENT_TOP, g.get("to_y").getAsInt());
        // same line as vanilla: −1.5 at y=320
        double at320 = 1.5 + (g.get("to_value").getAsDouble() - 1.5) * (320 + 64) / (TerrainWriter.GRADIENT_TOP + 64);
        assertEquals(-1.5, at320, 1e-9);
    }

    @Test
    void a_missing_vanilla_file_is_an_error_not_a_broken_datapack() {
        var e = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> TerrainWriter.files(spec(PLAIN), -64, 384, "x.json", p -> null));
        assertTrue(e.getMessage().contains("noise_settings/overworld"), e.getMessage());
    }

    private static final Path SERVER_JAR = Path.of("test-server/.cache/folia/versions/26.1.2/folia-26.1.2.jar");
    private static final Pattern ANY_REF = Pattern.compile("\"((?:minecraft|worldforge):[a-z0-9_]+/[a-z0-9_/]+)\"");

    /** Against the real 26.1.2 data (the Folia jar downloaded by test-server/levantar-folia.sh), when it is there. */
    @Test
    void every_built_in_terrain_against_the_real_vanilla_data() throws IOException {
        assumeTrue(Files.exists(SERVER_JAR), "sin " + SERVER_JAR + ": se salta");
        try (ZipFile z = new ZipFile(SERVER_JAR.toFile())) {
            Function<String, String> vanilla = p -> {
                var e = z.getEntry("data/minecraft/" + p + ".json");
                if (e == null) {
                    return null;
                }
                try (InputStream in = z.getInputStream(e)) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            };
            for (String base : TerrainSpec.BASES) {
                for (String id : Terrains.BUILT_IN) {
                    TerrainSpec t;
                    try (InputStream in = TerrainTest.class.getResourceAsStream("/terrains/" + id + ".yml")) {
                        t = spec(new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("base: overworld", "base: " + base));
                    }
                    Map<String, String> files = TerrainWriter.files(t, -64, 640, TerrainWriter.settingsPath(id, "towering"), vanilla);
                    for (var f : files.entrySet()) {
                        JsonElement parsed = JsonParser.parseString(f.getValue());
                        assertTrue(parsed.isJsonObject(), f.getKey());
                        Matcher m = ANY_REF.matcher(f.getValue());
                        while (m.find()) {
                            String ref = m.group(1);
                            assertFalse(ref.startsWith("minecraft:overworld"), "vanilla reference left in " + f.getKey() + ": " + ref);
                            if (ref.startsWith("worldforge:")) {
                                String file = "data/worldforge/worldgen/density_function/" + ref.substring("worldforge:".length()) + ".json";
                                assertTrue(files.containsKey(file), f.getKey() + " points to " + ref + ", which is not written");
                            }
                        }
                    }
                    for (String fn : new String[] {"offset", "factor", "jaggedness", "depth", "continents", "erosion", "ridges"}) {
                        assertTrue(files.keySet().stream().anyMatch(k -> k.endsWith("/" + fn + ".json")), base + "/" + id + " lacks " + fn);
                    }
                    String settings = files.get(TerrainWriter.settingsPath(id, "towering"));
                    assertFalse(settings.contains("\"from_y\":240"), "the top slide moved");
                    assertTrue(settings.contains("\"to_y\":" + TerrainWriter.GRADIENT_TOP), "depth gradient extended in the router");
                }
            }
        }
    }
}
