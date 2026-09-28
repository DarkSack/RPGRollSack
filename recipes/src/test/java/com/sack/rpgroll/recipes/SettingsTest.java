package com.sack.rpgroll.recipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {

    @Test
    void globSoloUsaElAsteriscoComoComodin() {
        var beds = Settings.glob("minecraft:*_bed");
        assertTrue(beds.matcher("minecraft:red_bed").matches());
        assertFalse(beds.matcher("minecraft:bedrock").matches());

        // Un punto o un paréntesis son literales, no regex.
        var literal = Settings.glob("extra/a.b");
        assertTrue(literal.matcher("extra/a.b").matches());
        assertFalse(literal.matcher("extra/axb").matches());

        assertTrue(Settings.glob("MINECRAFT:*").matcher("minecraft:torch").matches());
    }
}
