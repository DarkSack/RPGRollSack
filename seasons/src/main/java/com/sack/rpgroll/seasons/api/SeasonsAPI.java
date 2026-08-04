package com.sack.rpgroll.seasons.api;

import com.sack.rpgroll.seasons.climate.ClimateManager;
import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SeasonRegionManager;
import com.sack.rpgroll.seasons.core.WorldEvent;
import com.sack.rpgroll.seasons.core.WorldEventManager;
import com.sack.rpgroll.seasons.event.WorldEventEngine;
import com.sack.rpgroll.seasons.runtime.RegionSeasonResolver;
import com.sack.rpgroll.seasons.runtime.SeasonClockManager;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

/**
 * Punto de entrada público de RPGRoll-Seasons para cualquier otro addon —
 * pensado en primer lugar para un futuro RPGRoll-Farming/RPGRoll-Fishing
 * que quiera consultar "¿está permitido este cultivo/pez ahora acá?" sin
 * que Seasons tenga que saber nada de cultivos ni peces.
 */
public final class SeasonsAPI {

    private static SeasonsAPI instance;

    private final SeasonManager seasonManager;
    private final CalendarManager calendarManager;
    private final WorldEventManager worldEventManager;
    private final SeasonRegionManager regionManager;
    private final SeasonClockManager clockManager;
    private final RegionSeasonResolver regionResolver;
    private final ClimateManager climateManager;
    private final WorldEventEngine worldEventEngine;
    private final String defaultCalendarId;

    private SeasonsAPI(SeasonManager seasonManager, CalendarManager calendarManager,
            WorldEventManager worldEventManager, SeasonRegionManager regionManager, SeasonClockManager clockManager,
            RegionSeasonResolver regionResolver, ClimateManager climateManager, WorldEventEngine worldEventEngine,
            String defaultCalendarId) {
        this.seasonManager = seasonManager;
        this.calendarManager = calendarManager;
        this.worldEventManager = worldEventManager;
        this.regionManager = regionManager;
        this.clockManager = clockManager;
        this.regionResolver = regionResolver;
        this.climateManager = climateManager;
        this.worldEventEngine = worldEventEngine;
        this.defaultCalendarId = defaultCalendarId;
    }

    public static void init(SeasonManager seasonManager, CalendarManager calendarManager,
            WorldEventManager worldEventManager, SeasonRegionManager regionManager, SeasonClockManager clockManager,
            RegionSeasonResolver regionResolver, ClimateManager climateManager, WorldEventEngine worldEventEngine,
            String defaultCalendarId) {
        instance = new SeasonsAPI(seasonManager, calendarManager, worldEventManager, regionManager, clockManager,
                regionResolver, climateManager, worldEventEngine, defaultCalendarId);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /** @throws IllegalStateException si RPGRoll-Seasons todavía no está listo. */
    public static SeasonsAPI get() {

        if (instance == null) {
            throw new IllegalStateException("RPGRoll-Seasons todavía no está listo.");
        }

        return instance;
    }

    // ============ Consultas ============

    public Optional<Season> getCurrentSeason(Location location) {
        return regionResolver.resolveSeason(location);
    }

    public Optional<Season> getCurrentSeason(World world) {
        return getCurrentSeason(world.getSpawnLocation());
    }

    /** Temperatura efectiva en °C — bioma + estación + subestación + olas de calor activas. */
    public double getTemperature(Location location) {
        return climateManager.getTemperature(location);
    }

    /**
     * Atajo para un futuro RPGRoll-Farming/RPGRoll-Fishing: ¿la estación
     * actual en esta ubicación está entre las permitidas? No conoce nada de
     * cultivos/peces — solo compara contra el id de estación efectivo.
     */
    public boolean isSeasonAllowed(Location location, java.util.Set<String> allowedSeasonIds) {

        if (allowedSeasonIds == null || allowedSeasonIds.isEmpty()) {
            return true;
        }

        return getCurrentSeason(location).map(season -> allowedSeasonIds.contains(season.id())).orElse(false);
    }

    // ============ Control manual (admin) ============

    public boolean setSeason(World world, String seasonId) {

        Optional<Season> seasonOpt = seasonManager.get(seasonId);

        if (seasonOpt.isEmpty()) {
            return false;
        }

        String clockKey = RegionSeasonResolver.worldClockKey(world.getName());
        var state = clockManager.getOrCreate(clockKey, defaultCalendarId);

        return calendarManager.get(state.calendarId())
                .map(calendar -> {
                    int index = calendar.seasonIds().indexOf(seasonId);
                    if (index < 0) {
                        return false;
                    }
                    state.setSeasonIndex(index);
                    return true;
                })
                .orElse(false);
    }

    public void advanceSeason(World world) {

        String clockKey = RegionSeasonResolver.worldClockKey(world.getName());
        var state = clockManager.getOrCreate(clockKey, defaultCalendarId);

        calendarManager.get(state.calendarId())
                .ifPresent(calendar -> state.advanceSeason(calendar.seasonIds().size()));
    }

    public boolean triggerWorldEvent(String eventId, World world) {

        Optional<WorldEvent> eventOpt = worldEventManager.get(eventId);

        if (eventOpt.isEmpty()) {
            return false;
        }

        worldEventEngine.trigger(eventOpt.get(), world);
        return true;
    }

    // ============ Acceso interno (GUI/comandos del propio addon) ============

    public SeasonManager getSeasonManager() {
        return seasonManager;
    }

    public CalendarManager getCalendarManager() {
        return calendarManager;
    }

    public WorldEventManager getWorldEventManager() {
        return worldEventManager;
    }

    public SeasonRegionManager getRegionManager() {
        return regionManager;
    }

    public SeasonClockManager getClockManager() {
        return clockManager;
    }

    public WorldEventEngine getWorldEventEngine() {
        return worldEventEngine;
    }

}
