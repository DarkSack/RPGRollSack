package com.sack.rpgroll.gameplay.job;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PlacedBlockTrackerPackTest {

    @Test
    void neighboursAndMirroredCoordinatesNeverCollide() {
        Set<Long> seen = new HashSet<>();
        int count = 0;
        for (int x = -3; x <= 3; x++) {
            for (int y = -64; y <= 320; y += 64) {
                for (int z = -3; z <= 3; z++) {
                    seen.add(PlacedBlockTracker.pack(x, y, z));
                    count++;
                }
            }
        }
        assertEquals(count, seen.size());
    }

    @Test
    void farCoordinatesInsideTheWorldBorderStayDistinct() {
        assertNotEquals(PlacedBlockTracker.pack(29_999_999, 0, 0), PlacedBlockTracker.pack(-29_999_999, 0, 0));
        assertNotEquals(PlacedBlockTracker.pack(0, -64, 29_999_999), PlacedBlockTracker.pack(0, -64, -29_999_999));
        assertNotEquals(PlacedBlockTracker.pack(5, 10, 7), PlacedBlockTracker.pack(7, 10, 5));
    }

}
