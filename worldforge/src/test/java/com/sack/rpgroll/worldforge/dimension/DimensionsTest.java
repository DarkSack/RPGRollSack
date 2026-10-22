package com.sack.rpgroll.worldforge.dimension;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionsTest {

    @Test
    void only_combinations_whose_height_fits_are_accepted() {
        assertTrue(Dimensions.check("minas", "tall", "overworld").isEmpty());
        assertTrue(Dimensions.check("infierno", "nether", "nether").isEmpty());
        assertTrue(Dimensions.check("nada", "end", "void").isEmpty());
        assertEquals("generator", Dimensions.check("x", "overworld", "nether").orElseThrow().field(), "el Nether no cabe en −64…319 como tal");
        assertEquals("generator", Dimensions.check("x", "nether", "overworld").orElseThrow().field(), "el normal no cabe en 0…255");
        assertEquals("type", Dimensions.check("x", "marte", "void").orElseThrow().field());
        assertEquals("generator", Dimensions.check("x", "overworld", "magia").orElseThrow().field());
        assertTrue(Dimensions.check("alta", "towering", "terrain:montanas").isEmpty());
        assertTrue(Dimensions.check("baja", "overworld", "terrain:suave").isEmpty());
        assertEquals("generator", Dimensions.check("x", "nether", "terrain:montanas").orElseThrow().field(), "terrenos solo en tipos del normal");
        for (String bad : new String[] {"../x", "Mayus", "con-guion", "", "a".repeat(25)}) {
            assertEquals("id", Dimensions.check(bad, "overworld", "void").orElseThrow().field(), bad);
        }
    }

    @Test
    void every_valid_combination_writes_well_formed_json_with_the_right_type() {
        Yaml yaml = new Yaml(); // JSON is YAML: enough to check it parses and has the keys Minecraft reads
        for (var t : Dimensions.TYPES.values()) {
            for (var g : Dimensions.GENERATORS.values()) {
                if (Dimensions.check("x", t.id(), g.id()).isPresent()) {
                    continue;
                }
                Map<?, ?> json = yaml.load(Dimensions.dimensionJson(t.id(), g.id()));
                assertEquals(t.key(), json.get("type"));
                Map<?, ?> gen = (Map<?, ?>) json.get("generator");
                assertTrue(gen.get("type").equals("minecraft:flat") || gen.get("type").equals("minecraft:noise"), String.valueOf(gen));
            }
        }
    }

    @Test
    void the_tall_type_is_the_one_tested_on_a_real_server() {
        Map<?, ?> tall = new Yaml().load(DimensionsTest.class.getResourceAsStream("/dimension_type/tall.json"));
        assertEquals(-64, tall.get("min_y"));
        assertEquals(448, tall.get("height"));
        assertEquals(Dimensions.TYPES.get("tall").height(), tall.get("height"));
    }

    @Test
    void towering_and_terrain_dimensions() {
        Map<?, ?> towering = new Yaml().load(DimensionsTest.class.getResourceAsStream("/dimension_type/towering.json"));
        assertEquals(-64, towering.get("min_y"));
        assertEquals(Dimensions.TYPES.get("towering").height(), towering.get("height"));
        assertEquals(towering.get("height"), towering.get("logical_height"));
        Map<?, ?> json = new Yaml().load(Dimensions.dimensionJson("towering", "terrain:montanas"));
        assertEquals("worldforge:towering", json.get("type"));
        Map<?, ?> gen = (Map<?, ?>) json.get("generator");
        assertEquals("worldforge:montanas/towering", gen.get("settings"));
        assertEquals(Map.of("type", "minecraft:multi_noise", "preset", "minecraft:overworld"), gen.get("biome_source"));
    }
}
