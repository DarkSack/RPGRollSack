package com.sack.rpgroll.extras.temperature;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemperatureStateRangeTest {

    @Test
    void minIsInclusiveAndMaxIsExclusive() {
        TemperatureStateRange range = new TemperatureStateRange("cold", "Cold", 10.0, 20.0, List.of(), List.of());

        assertTrue(range.matches(10.0));
        assertFalse(range.matches(20.0));
        assertTrue(range.matches(19.9999));
        assertFalse(range.matches(9.9999));
    }

    @Test
    void valueWithinMiddleOfRangeMatches() {
        TemperatureStateRange range = new TemperatureStateRange("normal", "Normal", 30.0, 40.0, List.of(), List.of());

        assertTrue(range.matches(35.0));
    }
}
