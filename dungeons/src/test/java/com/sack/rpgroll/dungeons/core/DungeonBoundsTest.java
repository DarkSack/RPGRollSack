package com.sack.rpgroll.dungeons.core;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DungeonBoundsTest {

    @Test
    void constructorNormalizesInvertedBounds() {
        DungeonBounds bounds = new DungeonBounds("world", 10, 10, 10, 0, 0, 0);

        assertEquals(0, bounds.minX());
        assertEquals(10, bounds.maxX());
        assertEquals(0, bounds.minY());
        assertEquals(10, bounds.maxY());
        assertEquals(0, bounds.minZ());
        assertEquals(10, bounds.maxZ());
    }

    @Test
    void noneIsAZeroSizedBoundInDefaultWorld() {
        DungeonBounds none = DungeonBounds.none();
        assertEquals("world", none.world());
        assertEquals(0, none.minX());
        assertEquals(0, none.maxX());
    }

    @Test
    void containsReturnsFalseWhenWorldNameDiffers() {
        DungeonBounds bounds = new DungeonBounds("world", 0, 0, 0, 10, 10, 10);

        World otherWorld = mock(World.class);
        when(otherWorld.getName()).thenReturn("other");

        assertFalse(bounds.contains(new Location(otherWorld, 5, 5, 5)));
    }

    @Test
    void containsReturnsFalseWhenLocationWorldIsNull() {
        DungeonBounds bounds = new DungeonBounds("world", 0, 0, 0, 10, 10, 10);
        assertFalse(bounds.contains(new Location(null, 5, 5, 5)));
    }

    @Test
    void containsChecksAllAxesWithinBounds() {
        DungeonBounds bounds = new DungeonBounds("world", 0, 0, 0, 10, 10, 10);

        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        assertTrue(bounds.contains(new Location(world, 5, 5, 5)));
        assertTrue(bounds.contains(new Location(world, 0, 0, 0)));
        assertTrue(bounds.contains(new Location(world, 10, 10, 10)));
        assertFalse(bounds.contains(new Location(world, 11, 5, 5)));
        assertFalse(bounds.contains(new Location(world, 5, -0.1, 5)));
    }

    @Test
    void centerIsMidpointOfBounds() {
        DungeonBounds bounds = new DungeonBounds("world", 0, 0, 0, 10, 20, 30);
        DungeonPoint center = bounds.center();

        assertEquals(5.0, center.x());
        assertEquals(10.0, center.y());
        assertEquals(15.0, center.z());
        assertEquals("world", center.world());
    }
}
