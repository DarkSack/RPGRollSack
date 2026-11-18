package com.sack.rpgroll.mobs.healthbar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthBarsTest {

    @Test
    void filledSegments() {
        assertEquals(10, HealthBars.filled(100, 100, 10));
        assertEquals(5, HealthBars.filled(50, 100, 10));
        assertEquals(1, HealthBars.filled(0.5, 100, 10));
        assertEquals(0, HealthBars.filled(0, 100, 10));
        assertEquals(10, HealthBars.filled(150, 100, 10));
        assertEquals(0, HealthBars.filled(10, 0, 10));
    }
}
