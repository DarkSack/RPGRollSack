package com.sack.rpgroll.seasons.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldEventComponentTest {

    @Test
    void paramReturnsFallbackWhenKeyIsMissing() {
        WorldEventComponent component = new WorldEventComponent(WorldEventComponentType.MESSAGE, Map.of());
        assertEquals("fallback", component.param("text", "fallback"));
    }

    @Test
    void paramReturnsStoredValueWhenPresent() {
        WorldEventComponent component = new WorldEventComponent(WorldEventComponentType.MESSAGE,
                Map.of("text", "hello"));
        assertEquals("hello", component.param("text", "fallback"));
    }

    @Test
    void paramDoubleParsesValidNumbers() {
        WorldEventComponent component = new WorldEventComponent(WorldEventComponentType.SOUND,
                Map.of("volume", "1.5"));
        assertEquals(1.5, component.paramDouble("volume", 1.0));
    }

    @Test
    void paramDoubleFallsBackOnMalformedNumber() {
        WorldEventComponent component = new WorldEventComponent(WorldEventComponentType.SOUND,
                Map.of("volume", "not-a-number"));
        assertEquals(1.0, component.paramDouble("volume", 1.0));
    }

    @Test
    void paramDoubleFallsBackWhenKeyIsMissing() {
        WorldEventComponent component = new WorldEventComponent(WorldEventComponentType.SOUND, Map.of());
        assertEquals(2.5, component.paramDouble("volume", 2.5));
    }

    @Test
    void paramIntParsesValidNumbers() {
        WorldEventComponent component = new WorldEventComponent(WorldEventComponentType.PARTICLE,
                Map.of("count", "50"));
        assertEquals(50, component.paramInt("count", 20));
    }

    @Test
    void paramIntFallsBackOnMalformedNumber() {
        WorldEventComponent component = new WorldEventComponent(WorldEventComponentType.PARTICLE,
                Map.of("count", "abc"));
        assertEquals(20, component.paramInt("count", 20));
    }

    @Test
    void constructorCopiesParamsAsImmutableMap() {
        Map<String, String> mutable = new java.util.HashMap<>();
        mutable.put("text", "hello");

        WorldEventComponent component = new WorldEventComponent(WorldEventComponentType.MESSAGE, mutable);
        mutable.put("extra", "value");

        assertEquals(1, component.params().size());
    }

}
