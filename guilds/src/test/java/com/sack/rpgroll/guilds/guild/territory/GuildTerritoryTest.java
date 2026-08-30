package com.sack.rpgroll.guilds.guild.territory;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuildTerritoryTest {

    @Test
    void constructorNormalizesGivenCornersRegardlessOfOrder() {
        GuildTerritory territory = new GuildTerritory("base", "world", 10, 10, 10, 0, 0, 0);

        assertEquals(0, territory.minX());
        assertEquals(10, territory.maxX());
        assertEquals(0, territory.minY());
        assertEquals(10, territory.maxY());
    }

    @Test
    void volumeIsProductOfDimensions() {
        GuildTerritory territory = new GuildTerritory("base", "world", 0, 0, 0, 10, 5, 2);
        assertEquals(100.0, territory.volume());
    }

    @Test
    void containsReturnsFalseForDifferentWorld() {
        GuildTerritory territory = new GuildTerritory("base", "world", 0, 0, 0, 10, 10, 10);

        World other = mock(World.class);
        when(other.getName()).thenReturn("other");

        assertFalse(territory.contains(new Location(other, 5, 5, 5)));
    }

    @Test
    void containsReturnsFalseForNullLocationWorld() {
        GuildTerritory territory = new GuildTerritory("base", "world", 0, 0, 0, 10, 10, 10);
        assertFalse(territory.contains(new Location(null, 5, 5, 5)));
    }

    @Test
    void containsChecksBoundsInclusively() {
        GuildTerritory territory = new GuildTerritory("base", "world", 0, 0, 0, 10, 10, 10);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        assertTrue(territory.contains(new Location(world, 0, 0, 0)));
        assertTrue(territory.contains(new Location(world, 10, 10, 10)));
        assertFalse(territory.contains(new Location(world, 10.1, 5, 5)));
    }

    @Test
    void overlapsReturnsFalseForDifferentWorld() {
        GuildTerritory territory = new GuildTerritory("base", "world", 0, 0, 0, 10, 10, 10);
        assertFalse(territory.overlaps("other", 0, 0, 0, 10, 10, 10));
    }

    @Test
    void overlapsDetectsIntersectingCuboids() {
        GuildTerritory territory = new GuildTerritory("base", "world", 0, 0, 0, 10, 10, 10);

        assertTrue(territory.overlaps("world", 5, 5, 5, 15, 15, 15));
        assertTrue(territory.overlaps("world", -5, -5, -5, 0, 0, 0));
    }

    @Test
    void overlapsFalseWhenCuboidsAreSeparate() {
        GuildTerritory territory = new GuildTerritory("base", "world", 0, 0, 0, 10, 10, 10);

        assertFalse(territory.overlaps("world", 11, 0, 0, 20, 10, 10));
        assertFalse(territory.overlaps("world", 0, 11, 0, 10, 20, 10));
    }

    @Test
    void protectBlocksDefaultsToTrueAndOutsiderPvpDefaultsToFalse() {
        GuildTerritory territory = new GuildTerritory("base", "world", 0, 0, 0, 10, 10, 10);

        assertTrue(territory.protectBlocks());
        assertFalse(territory.allowOutsiderPvp());

        territory.setProtectBlocks(false);
        territory.setAllowOutsiderPvp(true);

        assertFalse(territory.protectBlocks());
        assertTrue(territory.allowOutsiderPvp());
    }
}
