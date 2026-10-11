package com.sack.rpgroll.extras.temperature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BodyTemperatureEngineTest {

    private static String stateAt(double ambient) {
        double body = BodyTemperatureEngine.bodyTarget(ambient);
        return TemperatureSettings.defaults().states().stream()
                .filter(range -> range.matches(body)).findFirst().orElseThrow().id();
    }

    @Test
    void ordinaryBiomesKeepTheBodyNormal() {
        assertEquals("normal", stateAt(16));  // llanura de día
        assertEquals("normal", stateAt(11));  // llanura de noche
        assertEquals("normal", stateAt(40));  // desierto o sabana de día
    }

    @Test
    void onlyExtremeWeatherHurts() {
        assertEquals("cold", stateAt(-5));                 // nieve de noche: aviso sin penalización
        assertEquals("hypothermia", stateAt(-10));         // nieve de noche con tormenta
        assertEquals("severe_hypothermia", stateAt(-27));  // pico helado de noche
        assertEquals("overheating", stateAt(46));          // desierto junto a lava
        assertEquals("hyperthermia", stateAt(65));         // Nether
    }

    @Test
    void theTargetNeverJumps() {
        assertTrue(BodyTemperatureEngine.bodyTarget(8) == BodyTemperatureEngine.bodyTarget(34));
    }
}
