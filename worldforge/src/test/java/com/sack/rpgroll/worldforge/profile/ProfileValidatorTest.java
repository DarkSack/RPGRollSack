package com.sack.rpgroll.worldforge.profile;

import com.sack.rpgroll.worldforge.TestCatalog;
import com.sack.rpgroll.worldforge.api.Problem;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileValidatorTest {

    private static Profile read(String yaml) {
        return ProfileCodec.read(yaml).value();
    }

    private static List<String> errorFields(Profile p) {
        return TestCatalog.validator().validate(p).stream().filter(Problem::isError).map(Problem::field).toList();
    }

    private static final String ORE = """
            ores:
            - id: extra
              block: diamond_ore
              veins-per-chunk: 2
              size: 6
              min-y: -64
              max-y: 16
            """;

    @Test
    void a_provider_cannot_promise_what_it_does_not_control() {
        // vanilla has no ore populator: asking for ores must fail, not be ignored.
        List<String> f = errorFields(read("schema: 1\nid: x\nprovider: vanilla\n" + ORE));
        assertEquals(List.of("ores"), f);
        // empty has no terrain to put structures in.
        assertEquals(List.of("structures"), errorFields(read("schema: 1\nid: x\nprovider: empty\nstructures: true\n")));
        // world-type is vanilla only.
        assertEquals(List.of("world-type"), errorFields(read("schema: 1\nid: x\nprovider: hybrid\nworld-type: amplified\n")));
    }

    @Test
    void the_same_ores_are_fine_where_the_provider_supports_them() {
        assertEquals(List.of(), errorFields(read("schema: 1\nid: x\nprovider: hybrid\n" + ORE)));
    }

    @Test
    void unknown_blocks_and_biomes_are_rejected() {
        List<String> f = errorFields(read("""
                schema: 1
                id: x
                provider: flat
                layers: [{block: unobtanium, height: 1}]
                biomes: [mordor]
                """));
        assertEquals(List.of("layers[0].block", "biomes[0]"), f);
    }

    @Test
    void heights_and_ranges_are_checked() {
        List<String> f = errorFields(read("""
                schema: 1
                id: x
                provider: hybrid
                spawn: {x: 0, y: 400, z: 0}
                platform: {block: stone, radius: 40, y: -100}
                ores:
                - id: bad
                  block: diamond_ore
                  veins-per-chunk: 100
                  size: 0
                  min-y: 50
                  max-y: 10
                  distribution: gaussian
                """));
        assertTrue(f.containsAll(List.of("spawn.y", "platform.radius", "platform.y", "ores[0].veins-per-chunk",
                "ores[0].size", "ores[0].min-y", "ores[0].distribution")), f.toString());
    }

    @Test
    void nether_has_its_own_height() {
        List<String> f = errorFields(read("schema: 1\nid: x\nprovider: empty\nenvironment: nether\nspawn: {x: 0, y: -10, z: 0}\n"));
        assertEquals(List.of("spawn.y"), f);
    }

    @Test
    void flat_needs_layers_and_layers_must_fit() {
        assertEquals(List.of("layers"), errorFields(read("schema: 1\nid: x\nprovider: flat\n")));
        assertEquals(List.of("layers"), errorFields(read("schema: 1\nid: x\nprovider: flat\nlayers: [{block: stone, height: 500}]\n")));
    }

    @Test
    void repeated_ore_ids_and_contradictory_biomes() {
        List<String> f = errorFields(read("""
                schema: 1
                id: x
                provider: hybrid
                ores:
                - {id: a, block: iron_ore, veins-per-chunk: 1, size: 3, min-y: 0, max-y: 10, biomes: [plains], exclude-biomes: [plains]}
                - {id: a, block: iron_ore, veins-per-chunk: 1, size: 3, min-y: 0, max-y: 10}
                """));
        assertTrue(f.contains("ores[1].id"), f.toString());
        assertTrue(f.contains("ores[0].exclude-biomes[0]"), f.toString());
    }

    @Test
    void unknown_provider_lists_the_real_ones() {
        List<Problem> p = TestCatalog.validator().validate(read("schema: 1\nid: x\nprovider: magic\n"));
        assertEquals("provider", p.getFirst().field());
        assertTrue(p.getFirst().message().contains("empty, flat, hybrid, plugin, terrain, vanilla"), p.getFirst().message());
    }

    @Test
    void bad_ids_are_rejected() {
        assertEquals(List.of("id"), errorFields(read("schema: 1\nid: ../evil\nprovider: vanilla\n")));
        assertEquals(List.of("id"), errorFields(read("schema: 1\nid: Mayúsculas\nprovider: vanilla\n")));
    }
}
