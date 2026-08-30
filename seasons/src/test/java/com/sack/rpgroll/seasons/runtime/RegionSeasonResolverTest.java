package com.sack.rpgroll.seasons.runtime;

import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.DurationUnit;
import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SeasonRegion;
import com.sack.rpgroll.seasons.core.SeasonRegionManager;
import com.sack.rpgroll.seasons.core.SeasonRegionOverrideMode;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionSeasonResolverTest {

    @Mock
    private SeasonRegionManager regionManager;

    @Mock
    private SeasonManager seasonManager;

    @Mock
    private CalendarManager calendarManager;

    @Mock
    private World world;

    private SeasonClockManager clockManager;
    private RegionSeasonResolver resolver;
    private Location location;

    private Season season(String id) {
        return new Season(id, null, null, null, null, 1, DurationUnit.MINECRAFT_DAYS, null, List.of(), Map.of(),
                Set.of(), List.of(), null, List.of(), 0, Set.of());
    }

    @BeforeEach
    void setUp() {
        clockManager = new SeasonClockManager(seasonManager, calendarManager);
        resolver = new RegionSeasonResolver(regionManager, seasonManager, clockManager, "default");

        lenient().when(world.getName()).thenReturn("world");
        location = new Location(world, 5, 64, 5);
        lenient().when(regionManager.getAll()).thenReturn(List.of());
        lenient().when(calendarManager.get(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
    }

    @Test
    void resolveSeasonReturnsEmptyWhenLocationHasNoWorld() {
        Location noWorld = new Location(null, 0, 0, 0);
        assertTrue(resolver.resolveSeason(noWorld).isEmpty());
    }

    @Test
    void pinnedSeasonRegionOverridesWithoutTouchingTheClock() {
        SeasonRegion region = new SeasonRegion("cap", "world", 0, 0, 0, 10, 128, 10,
                SeasonRegionOverrideMode.PINNED_SEASON, "eternal-winter", null);
        when(regionManager.getAll()).thenReturn(List.of(region));
        when(seasonManager.get("eternal-winter")).thenReturn(Optional.of(season("eternal-winter")));

        Optional<Season> resolved = resolver.resolveSeason(location);

        assertTrue(resolved.isPresent());
        assertEquals("eternal-winter", resolved.get().id());
        assertTrue(clockManager.allClocks().isEmpty());
    }

    @Test
    void pinnedCalendarRegionCreatesItsOwnClockKeyedByRegionId() {
        SeasonRegion region = new SeasonRegion("cap", "world", 0, 0, 0, 10, 128, 10,
                SeasonRegionOverrideMode.PINNED_CALENDAR, null, "polar-calendar");
        when(regionManager.getAll()).thenReturn(List.of(region));

        resolver.resolveSeason(location);

        assertTrue(clockManager.get("region:cap").isPresent());
        assertEquals("polar-calendar", clockManager.get("region:cap").get().calendarId());
    }

    @Test
    void outsideAnyRegionFallsBackToTheWorldClock() {
        resolver.resolveSeason(location);

        assertTrue(clockManager.get("world:world").isPresent());
        assertEquals("default", clockManager.get("world:world").get().calendarId());
    }

    @Test
    void resolveClockKeyReturnsRegionKeyOnlyForPinnedCalendarMode() {
        SeasonRegion pinnedSeasonRegion = new SeasonRegion("cap", "world", 0, 0, 0, 10, 128, 10,
                SeasonRegionOverrideMode.PINNED_SEASON, "eternal-winter", null);
        when(regionManager.getAll()).thenReturn(List.of(pinnedSeasonRegion));

        assertEquals("world:world", resolver.resolveClockKey(location));
    }

    @Test
    void resolveClockKeyReturnsWorldKeyWhenOutsideAnyRegion() {
        assertEquals("world:world", resolver.resolveClockKey(location));
    }

    @Test
    void resolveClockKeyReturnsNullWhenLocationHasNoWorld() {
        Location noWorld = new Location(null, 0, 0, 0);
        assertEquals(null, resolver.resolveClockKey(noWorld));
    }

    @Test
    void worldClockKeyHelperPrefixesWithWorldColon() {
        assertEquals("world:overworld", RegionSeasonResolver.worldClockKey("overworld"));
    }

}
