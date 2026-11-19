package com.sack.rpgroll.crates.treasure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreasureHintTest {

    @Test
    void eightDirectionsWithNorthAsMinusZ() {

        assertEquals("n", TreasureHint.direction(0, -10));
        assertEquals("ne", TreasureHint.direction(10, -10));
        assertEquals("e", TreasureHint.direction(10, 0));
        assertEquals("se", TreasureHint.direction(10, 10));
        assertEquals("s", TreasureHint.direction(0, 10));
        assertEquals("so", TreasureHint.direction(-10, 10));
        assertEquals("o", TreasureHint.direction(-10, 0));
        assertEquals("no", TreasureHint.direction(-10, -10));
        assertEquals("n", TreasureHint.direction(2, -30), "casi norte sigue siendo norte");
    }

    @Test
    void horizontalDistanceRounded() {

        assertEquals(5, TreasureHint.distance(3, 4));
        assertEquals(0, TreasureHint.distance(0, 0));
        assertEquals(141, TreasureHint.distance(-100, 100));
    }

}
