package com.sack.rpgroll.seasons.runtime;

import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SeasonRegion;
import com.sack.rpgroll.seasons.core.SeasonRegionManager;
import com.sack.rpgroll.seasons.core.SeasonRegionOverrideMode;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

/**
 * Decide la estación efectiva en un punto del mundo — chequea primero si
 * cae dentro de una {@link SeasonRegion} con override, y si no, cae al
 * reloj normal del mundo ({@code "world:<nombre>"}, creado bajo demanda
 * con el calendario por defecto del plugin).
 */
public class RegionSeasonResolver {

    private final SeasonRegionManager regionManager;
    private final SeasonManager seasonManager;
    private final SeasonClockManager clockManager;
    private final String defaultCalendarId;

    public RegionSeasonResolver(SeasonRegionManager regionManager, SeasonManager seasonManager,
            SeasonClockManager clockManager, String defaultCalendarId) {
        this.regionManager = regionManager;
        this.seasonManager = seasonManager;
        this.clockManager = clockManager;
        this.defaultCalendarId = defaultCalendarId;
    }

    public Optional<Season> resolveSeason(Location location) {

        World world = location.getWorld();

        if (world == null) {
            return Optional.empty();
        }

        Optional<SeasonRegion> regionOpt = findRegion(world.getName(), location.getX(), location.getY(),
                location.getZ());

        if (regionOpt.isPresent()) {

            SeasonRegion region = regionOpt.get();

            if (region.overrideMode() == SeasonRegionOverrideMode.PINNED_SEASON && region.pinnedSeasonId() != null) {
                return seasonManager.get(region.pinnedSeasonId());
            }

            if (region.overrideMode() == SeasonRegionOverrideMode.PINNED_CALENDAR && region.pinnedCalendarId() != null) {
                CalendarState state = clockManager.getOrCreate(region.clockKey(), region.pinnedCalendarId());
                return clockManager.resolveCurrentSeason(state);
            }
        }

        CalendarState worldState = clockManager.getOrCreate(worldClockKey(world.getName()), defaultCalendarId);
        return clockManager.resolveCurrentSeason(worldState);
    }

    public String resolveClockKey(Location location) {

        World world = location.getWorld();

        if (world == null) {
            return null;
        }

        Optional<SeasonRegion> regionOpt = findRegion(world.getName(), location.getX(), location.getY(),
                location.getZ());

        if (regionOpt.isPresent() && regionOpt.get().overrideMode() == SeasonRegionOverrideMode.PINNED_CALENDAR) {
            return regionOpt.get().clockKey();
        }

        return worldClockKey(world.getName());
    }

    private Optional<SeasonRegion> findRegion(String worldName, double x, double y, double z) {
        return regionManager.getAll().stream()
                .filter(region -> region.contains(worldName, x, y, z))
                .findFirst();
    }

    public static String worldClockKey(String worldName) {
        return "world:" + worldName;
    }

}
