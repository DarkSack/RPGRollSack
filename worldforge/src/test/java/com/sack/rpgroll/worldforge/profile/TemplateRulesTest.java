package com.sack.rpgroll.worldforge.profile;

import com.sack.rpgroll.worldforge.TestCatalog;
import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.api.Result;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateRulesTest {

    private static final String BASE = "schema: 1\nid: x\nprovider: flat\nlayers: [{block: stone, height: 4}]\n";

    private static Profile read(String yaml) {
        Result<Profile> r = ProfileCodec.read(yaml);
        assertTrue(r.isOk(), r.toString());
        return r.value();
    }

    private static List<String> errorFields(String yaml) {
        return TestCatalog.validator().validate(read(yaml)).stream().filter(Problem::isError).map(Problem::field).toList();
    }

    @Test
    void defaults_and_round_trip() {
        Profile p = read(BASE + "templates: [{id: t1, structure: torre, rarity: 40}]\n");
        TemplateRule t = p.templates().getFirst();
        assertEquals("surface", t.placement());
        assertEquals("random", t.rotation());
        assertNull(t.minY());
        assertNull(t.maxY());
        assertTrue(t.enabled());
        String text = ProfileCodec.write(p);
        assertFalse(text.contains("min-y"), "an unset height is not written as a number");
        assertEquals(p, read(text));
        assertEquals(List.of(), errorFields(text));
    }

    @Test
    void profiles_without_templates_keep_their_text_and_version() {
        // Published presets are versioned by the hash of their text: adding the field must not change it.
        Profile p = read(BASE);
        assertFalse(ProfileCodec.write(p).contains("templates"));
    }

    @Test
    void only_providers_with_a_populator_take_templates() {
        String rule = "templates: [{id: t1, structure: torre, rarity: 40}]\n";
        assertEquals(List.of("templates"), errorFields("schema: 1\nid: x\nprovider: vanilla\n" + rule));
        assertEquals(List.of(), errorFields("schema: 1\nid: x\nprovider: hybrid\n" + rule));
        assertEquals(List.of(), errorFields("schema: 1\nid: x\nprovider: empty\n" + rule));
    }

    @Test
    void bad_rules_are_rejected_field_by_field() {
        assertEquals(List.of("templates[0].structure"), errorFields(BASE + "templates: [{id: t1, structure: nada, rarity: 4}]\n"));
        assertEquals(List.of("templates[0].structure"), errorFields(BASE + "templates: [{id: t1, structure: ../torre, rarity: 4}]\n"));
        assertEquals(List.of("templates[0].structure"), errorFields(BASE + "templates: [{id: t1, structure: enorme, rarity: 4}]\n"));
        assertEquals(List.of("templates[0].structure"), errorFields(BASE + "templates: [{id: t1, structure: alta, rarity: 4}]\n"));
        assertEquals(List.of("templates[0].rarity"), errorFields(BASE + "templates: [{id: t1, structure: torre, rarity: 0}]\n"));
        assertEquals(List.of("templates[0].placement"), errorFields(BASE + "templates: [{id: t1, structure: torre, rarity: 4, placement: cielo}]\n"));
        assertEquals(List.of("templates[0].rotation"), errorFields(BASE + "templates: [{id: t1, structure: torre, rarity: 4, rotation: 45}]\n"));
        assertEquals(List.of("templates[0].y-offset"), errorFields(BASE + "templates: [{id: t1, structure: torre, rarity: 4, y-offset: 99}]\n"));
        assertEquals(List.of("templates[0].min-y"), errorFields(BASE + "templates: [{id: t1, structure: torre, rarity: 4, min-y: 10, max-y: 0}]\n"));
        assertEquals(List.of("templates[0]"), errorFields(BASE + "templates: [{id: t1, structure: torre, rarity: 4, max-y: 400}]\n"));
        assertEquals(List.of("templates[1].id"), errorFields(BASE
                + "templates: [{id: t1, structure: torre, rarity: 4}, {id: t1, structure: sala, rarity: 4}]\n"));
        assertEquals(List.of("templates[0].exclude-biomes[0]"), errorFields(BASE
                + "templates: [{id: t1, structure: torre, rarity: 4, biomes: [plains], exclude-biomes: [plains]}]\n"));
        // The bottom of a 9-high template at 315 would end at 323, past the top of the world (319).
        assertEquals(List.of("templates[0].min-y"), errorFields(BASE
                + "templates: [{id: t1, structure: torre, rarity: 4, placement: underground, min-y: 315}]\n"));
    }

    @Test
    void a_rule_without_its_keys_is_a_read_error() {
        assertFalse(ProfileCodec.read(BASE + "templates: [{id: t1, rarity: 4}]\n").isOk());
    }
}
