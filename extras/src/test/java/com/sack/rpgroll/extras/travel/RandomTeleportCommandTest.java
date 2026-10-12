package com.sack.rpgroll.extras.travel;

import com.sack.rpgroll.extras.travel.RandomTeleportCommand.Zone;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomTeleportCommandTest {

    @Test
    void everySpotFallsInsideTheRing() {
        Zone zone = new Zone(250, 3000);
        Random random = new Random(7);

        for (int i = 0; i < 2000; i++) {
            int[] spot = zone.pick(random, 100, -40);
            double distance = Math.hypot(spot[0] + 0.5 - 100, spot[1] + 0.5 + 40);
            assertTrue(distance >= 249 && distance <= 3001, "a " + distance + " bloques");
        }
    }

    @Test
    void theWorldBorderShrinksTheRing() {
        assertEquals(new Zone(250, 998), new Zone(250, 3000).within(998));
        // Un borde más pequeño que la zona muerta: se cae donde quepa, dentro del borde.
        assertEquals(new Zone(99, 100), new Zone(250, 3000).within(100));
        assertEquals(new Zone(250, 3000), new Zone(250, 3000).within(30_000_000));
    }
}
