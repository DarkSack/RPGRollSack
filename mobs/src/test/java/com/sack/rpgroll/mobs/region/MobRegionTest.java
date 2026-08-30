package com.sack.rpgroll.mobs.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MobRegionTest {

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new MobRegion("", "world", 0, 0, 0, 1, 1, 1));
    }

    @Test
    void nullWorldIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new MobRegion("arena", null, 0, 0, 0, 1, 1, 1));
    }

    @Test
    void constructorNormalizesInvertedBounds() {
        MobRegion region = new MobRegion("arena", "world", 10, 10, 10, 0, 0, 0);

        assertEquals(0, region.minX());
        assertEquals(10, region.maxX());
        assertEquals(0, region.minY());
        assertEquals(10, region.maxY());
        assertEquals(0, region.minZ());
        assertEquals(10, region.maxZ());
    }

    @Test
    void containsReturnsFalseWhenWorldNameDiffers() {
        MobRegion region = new MobRegion("arena", "world", 0, 0, 0, 10, 10, 10);

        World otherWorld = mock(World.class);
        when(otherWorld.getName()).thenReturn("other");
        Location location = new Location(otherWorld, 5, 5, 5);

        assertFalse(region.contains(location));
    }

    @Test
    void containsReturnsFalseWhenLocationWorldIsNull() {
        MobRegion region = new MobRegion("arena", "world", 0, 0, 0, 10, 10, 10);
        Location location = new Location(null, 5, 5, 5);

        assertFalse(region.contains(location));
    }

    @Test
    void containsChecksAllAxesWithinBounds() {
        MobRegion region = new MobRegion("arena", "world", 0, 0, 0, 10, 10, 10);

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        assertTrue(region.contains(new Location(world, 5, 5, 5)));
        assertTrue(region.contains(new Location(world, 0, 0, 0)));
        assertTrue(region.contains(new Location(world, 10, 10, 10)));
        assertFalse(region.contains(new Location(world, 11, 5, 5)));
        assertFalse(region.contains(new Location(world, 5, -1, 5)));
        assertFalse(region.contains(new Location(world, 5, 5, 10.1)));
    }
}
