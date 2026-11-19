package com.sack.rpgroll.crates.treasure;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreasureSpotTest {

    private static final Treasure.Placement RING = new Treasure.Placement(List.of(), Set.of(), 100, 300, 3);

    @Test
    void picksInsideTheRing() {

        Random random = new Random(5);
        for (int i = 0; i < 1000; i++) {
            int[] spot = TreasureSpot.pick(1000, -2000, RING, random, (x, z) -> true, 10).orElseThrow();
            double distance = Math.hypot(spot[0] - 1000, spot[1] + 2000);
            assertTrue(distance >= 99.5 && distance <= 300.5, "fuera del anillo: " + distance);
        }
    }

    @Test
    void retriesUntilValid() {

        AtomicInteger calls = new AtomicInteger();
        Optional<int[]> spot = TreasureSpot.pick(0, 0, RING, new Random(1), (x, z) -> calls.incrementAndGet() == 4, 10);
        assertTrue(spot.isPresent());
        assertEquals(4, calls.get());
    }

    @Test
    void givesUpAfterTries() {

        AtomicInteger calls = new AtomicInteger();
        Optional<int[]> spot = TreasureSpot.pick(0, 0, RING, new Random(1), (x, z) -> {
            calls.incrementAndGet();
            return false;
        }, 12);
        assertTrue(spot.isEmpty());
        assertEquals(12, calls.get());
    }

}
