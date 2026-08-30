package com.sack.rpgroll.traps.location;

import com.sack.rpgroll.traps.core.TrapState;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlacedTrapTest {

    @Test
    void nullStateDefaultsToArmed() {
        PlacedTrap trap = new PlacedTrap("p1", "trapId", "world", 0, 0, 0, null, null, null, null, -1, 0);

        assertEquals(TrapState.ARMED, trap.state());
    }

    @Test
    void constructorRejectsNullRequiredFields() {
        assertThrows(NullPointerException.class,
                () -> new PlacedTrap(null, "t", "world", 0, 0, 0, null, null, null, TrapState.ARMED, -1, 0));
        assertThrows(NullPointerException.class,
                () -> new PlacedTrap("p1", null, "world", 0, 0, 0, null, null, null, TrapState.ARMED, -1, 0));
        assertThrows(NullPointerException.class,
                () -> new PlacedTrap("p1", "t", null, 0, 0, 0, null, null, null, TrapState.ARMED, -1, 0));
    }

    @Test
    void isZoneOnlyWhenAllSecondCoordinatesPresent() {
        PlacedTrap point = new PlacedTrap("p1", "t", "world", 0, 0, 0, null, null, null, TrapState.ARMED, -1, 0);
        PlacedTrap zone = new PlacedTrap("p2", "t", "world", 0, 0, 0, 5, 5, 5, TrapState.ARMED, -1, 0);

        assertFalse(point.isZone());
        assertTrue(zone.isZone());
    }

    @Test
    void containsMatchesExactPointForNonZoneTrap() {
        PlacedTrap point = new PlacedTrap("p1", "t", "world", 5, 6, 7, null, null, null, TrapState.ARMED, -1, 0);

        assertTrue(point.contains(locationAt("world", 5, 6, 7)));
        assertFalse(point.contains(locationAt("world", 5, 6, 8)));
    }

    @Test
    void containsChecksWorldNameFirst() {
        PlacedTrap point = new PlacedTrap("p1", "t", "world", 5, 6, 7, null, null, null, TrapState.ARMED, -1, 0);

        assertFalse(point.contains(locationAt("otherworld", 5, 6, 7)));
    }

    @Test
    void containsHandlesNullWorldOnLocationGracefully() {
        PlacedTrap point = new PlacedTrap("p1", "t", "world", 5, 6, 7, null, null, null, TrapState.ARMED, -1, 0);
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(null);

        assertFalse(point.contains(location));
    }

    @Test
    void containsIncludesBoundaryBlocksOfAZone() {
        PlacedTrap zone = new PlacedTrap("p2", "t", "world", 0, 0, 0, 10, 10, 10, TrapState.ARMED, -1, 0);

        assertTrue(zone.contains(locationAt("world", 0, 0, 0)));
        assertTrue(zone.contains(locationAt("world", 10, 10, 10)));
        assertTrue(zone.contains(locationAt("world", 5, 5, 5)));
        assertFalse(zone.contains(locationAt("world", 11, 5, 5)));
        assertFalse(zone.contains(locationAt("world", -1, 5, 5)));
    }

    @Test
    void containsHandlesInvertedZoneCornersCorrectly() {
        // x2/y2/z2 smaller than x/y/z — min/max must still be resolved correctly.
        PlacedTrap zone = new PlacedTrap("p3", "t", "world", 10, 10, 10, 0, 0, 0, TrapState.ARMED, -1, 0);

        assertTrue(zone.contains(locationAt("world", 5, 5, 5)));
        assertTrue(zone.contains(locationAt("world", 0, 0, 0)));
        assertTrue(zone.contains(locationAt("world", 10, 10, 10)));
    }

    @Test
    void withStateChargesAndCooldownProduceIndependentCopies() {
        PlacedTrap original = new PlacedTrap("p1", "t", "world", 0, 0, 0, null, null, null, TrapState.ARMED, 3, 0);

        PlacedTrap stateChanged = original.withState(TrapState.TRIGGERED);
        PlacedTrap chargesChanged = original.withCharges(1);
        PlacedTrap cooldownChanged = original.withCooldownExpiresAt(9999L);

        assertEquals(TrapState.ARMED, original.state());
        assertEquals(TrapState.TRIGGERED, stateChanged.state());
        assertEquals(3, original.chargesRemaining());
        assertEquals(1, chargesChanged.chargesRemaining());
        assertEquals(0, original.cooldownExpiresAtMillis());
        assertEquals(9999L, cooldownChanged.cooldownExpiresAtMillis());
    }

    @Test
    void tryFindReturnsFirstMatchingTrap() {
        PlacedTrap first = new PlacedTrap("p1", "t", "world", 0, 0, 0, null, null, null, TrapState.ARMED, -1, 0);
        PlacedTrap second = new PlacedTrap("p2", "t", "world", 5, 5, 5, null, null, null, TrapState.ARMED, -1, 0);

        Optional<PlacedTrap> found = PlacedTrap.tryFind(List.of(first, second), locationAt("world", 5, 5, 5));

        assertTrue(found.isPresent());
        assertEquals("p2", found.get().placementId());
    }

    @Test
    void tryFindReturnsEmptyWhenNothingMatches() {
        PlacedTrap first = new PlacedTrap("p1", "t", "world", 0, 0, 0, null, null, null, TrapState.ARMED, -1, 0);

        assertTrue(PlacedTrap.tryFind(List.of(first), locationAt("world", 99, 99, 99)).isEmpty());
    }

    private Location locationAt(String worldName, int x, int y, int z) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(worldName);

        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(x);
        when(location.getBlockY()).thenReturn(y);
        when(location.getBlockZ()).thenReturn(z);

        return location;
    }
}
