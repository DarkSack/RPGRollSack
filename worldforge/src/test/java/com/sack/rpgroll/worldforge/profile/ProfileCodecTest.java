package com.sack.rpgroll.worldforge.profile;

import com.sack.rpgroll.worldforge.TestCatalog;
import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.api.Result;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileCodecTest {

    static List<String> builtIn() {
        return ProfileService.BUILT_IN;
    }

    static String resource(String id) throws IOException {
        try (InputStream in = ProfileCodecTest.class.getResourceAsStream("/presets/" + id + ".yml")) {
            assertNotNull(in, "falta el preset " + id + " en el jar");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @ParameterizedTest
    @MethodSource("builtIn")
    void every_built_in_preset_reads_and_validates_without_problems(String id) throws IOException {
        Result<Profile> r = ProfileCodec.read(resource(id));
        assertTrue(r.isOk(), () -> id + ": " + r.problems());
        assertEquals(List.of(), r.problems(), id + " no debería tener avisos");
        assertEquals(id, r.value().id());
        List<Problem> problems = TestCatalog.validator().validate(r.value());
        assertEquals(List.of(), problems, id + ": " + problems);
    }

    @ParameterizedTest
    @MethodSource("builtIn")
    void writing_is_deterministic_and_round_trips(String id) throws IOException {
        Profile p = ProfileCodec.read(resource(id)).value();
        String once = ProfileCodec.write(p);
        Profile back = ProfileCodec.read(once).value();
        assertEquals(p, back);
        assertEquals(once, ProfileCodec.write(back));
        assertEquals(ProfileCodec.hash(p), ProfileCodec.hash(back));
    }

    @Test
    void unknown_keys_warn_but_do_not_fail() {
        Result<Profile> r = ProfileCodec.read("schema: 1\nid: x\nprovider: vanilla\ncolour: blue\n");
        assertTrue(r.isOk());
        assertTrue(r.problems().stream().anyMatch(p -> !p.isError() && p.field().equals("colour")));
    }

    @Test
    void missing_required_keys_are_errors() {
        Result<Profile> r = ProfileCodec.read("name: Sin nada\n");
        assertFalse(r.isOk());
        List<String> fields = r.errors().stream().map(Problem::field).toList();
        assertTrue(fields.containsAll(List.of("schema", "id", "provider")), fields.toString());
    }

    @Test
    void a_newer_schema_is_refused() {
        Result<Profile> r = ProfileCodec.read("schema: 99\nid: x\nprovider: vanilla\n");
        assertFalse(r.isOk());
        assertEquals("schema", r.errors().getFirst().field());
    }

    @Test
    void wrong_types_name_the_field() {
        Result<Profile> r = ProfileCodec.read("""
                schema: 1
                id: x
                provider: flat
                seed: muchas
                structures: quizá
                layers:
                - block: stone
                  height: alto
                """);
        assertFalse(r.isOk());
        List<String> fields = r.errors().stream().map(Problem::field).toList();
        assertTrue(fields.contains("seed"), fields.toString());
        assertTrue(fields.contains("structures"), fields.toString());
        assertTrue(fields.contains("layers[0].height"), fields.toString());
    }

    @Test
    void malformed_yaml_is_an_error_not_an_exception() {
        Result<Profile> r = ProfileCodec.read("schema: 1\nid: [sin cerrar\n");
        assertFalse(r.isOk());
        assertTrue(r.errors().getFirst().message().startsWith("YAML mal formado"));
    }

    @Test
    void block_and_biome_names_are_normalized() {
        Profile p = ProfileCodec.read("""
                schema: 1
                id: x
                provider: flat
                layers: [{block: Stone, height: 1}]
                biomes: [plains]
                """).value();
        assertEquals("minecraft:stone", p.layers().getFirst().block());
        assertEquals(List.of("minecraft:plains"), p.biomes());
        assertNull(p.structures(), "sin clave = lo que decida el proveedor");
    }
}
