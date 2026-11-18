package com.sack.rpgroll.enchantments.station;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StationMathTest {

    @Test
    void superEnchanterCostGrowsPerExtraLevel() {
        assertEquals(15, SuperEnchanter.xpFor(6, 5, 15, 10));
        assertEquals(25, SuperEnchanter.xpFor(7, 5, 15, 10));
        assertEquals(35, SuperEnchanter.xpFor(8, 5, 15, 10));
    }

    @Test
    void infusionsRoundTrip() {
        Map<String, Integer> parsed = Infuser.parse("night_vision:0;speed:1;roto");
        assertEquals(Map.of("night_vision", 0, "speed", 1), parsed);
        assertEquals("night_vision:0;speed:1", Infuser.write(parsed));
        assertEquals(Map.of(), Infuser.parse(null));
    }
}
