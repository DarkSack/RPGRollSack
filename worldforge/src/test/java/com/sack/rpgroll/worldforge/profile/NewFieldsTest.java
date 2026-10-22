package com.sack.rpgroll.worldforge.profile;

import com.sack.rpgroll.worldforge.TestCatalog;
import com.sack.rpgroll.worldforge.api.Problem;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Another plugin's generator ({@code provider: plugin}) and {@code player-gravity}. */
class NewFieldsTest {

    private static List<String> errorFields(String yaml) {
        var r = ProfileCodec.read(yaml);
        assertTrue(r.isOk(), r.toString());
        return TestCatalog.validator().validate(r.value()).stream().filter(Problem::isError).map(Problem::field).toList();
    }

    @Test
    void another_plugins_generator() {
        assertEquals(List.of(), errorFields("schema: 1\nid: x\nprovider: plugin\ngenerator: Terra:OVERWORLD\nseed: 5\n"));
        assertEquals(List.of(), errorFields("schema: 1\nid: x\nprovider: plugin\ngenerator: OtroGenerador\n"));
        assertEquals(List.of("generator"), errorFields("schema: 1\nid: x\nprovider: plugin\n"));
        assertEquals(List.of("generator"), errorFields("schema: 1\nid: x\nprovider: plugin\ngenerator: NoInstalado:a\n"));
        assertEquals(List.of("generator"), errorFields("schema: 1\nid: x\nprovider: plugin\ngenerator: 'Terra: con espacio'\n"));
        assertEquals(List.of("generator"), errorFields("schema: 1\nid: x\nprovider: vanilla\ngenerator: Terra\n"));
        // WorldForge does not put its ores or templates inside another plugin's generator.
        assertEquals(List.of("ores"), errorFields("schema: 1\nid: x\nprovider: plugin\ngenerator: Terra\n"
                + "ores: [{id: a, block: diamond_ore, veins-per-chunk: 1, size: 3, min-y: 0, max-y: 10}]\n"));
    }

    @Test
    void player_gravity() {
        String base = "schema: 1\nid: x\nprovider: flat\nlayers: [{block: stone, height: 4}]\n";
        Profile p = ProfileCodec.read(base + "player-gravity: 0.5\n").value();
        assertEquals(0.5, p.playerGravity());
        assertEquals(p, ProfileCodec.read(ProfileCodec.write(p)).value());
        assertEquals(List.of(), errorFields(base + "player-gravity: 2\n"));
        assertEquals(List.of("player-gravity"), errorFields(base + "player-gravity: 0.05\n"));
        assertEquals(List.of("player-gravity"), errorFields(base + "player-gravity: 9\n"));
        assertFalse(ProfileCodec.read(base + "player-gravity: mucha\n").isOk());
        assertFalse(ProfileCodec.write(ProfileCodec.read(base).value()).contains("gravity"), "unset is not written");
    }
}
