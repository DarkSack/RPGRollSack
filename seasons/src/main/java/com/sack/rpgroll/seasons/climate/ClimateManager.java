package com.sack.rpgroll.seasons.climate;

import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.SubSeason;
import com.sack.rpgroll.seasons.runtime.RegionSeasonResolver;
import com.sack.rpgroll.seasons.runtime.SeasonClockManager;

import org.bukkit.Location;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resuelve temperatura efectiva en un punto (bioma + estación + eventuales
 * olas de calor activas) y guarda el estado efímero de "ola de
 * calor"/"tormenta eléctrica" por reloj — {@code WeatherTickTask} lo
 * re-sortea periódicamente, nunca se persiste (se pierde en un reinicio,
 * lo cual es aceptable para algo tan efímero).
 */
public class ClimateManager {

    private final RegionSeasonResolver regionResolver;
    private final SeasonClockManager clockManager;
    private final BiomeTemperatureTable biomeTemperatureTable;
    private final Random random = new Random();

    private final Map<String, Boolean> heatwaveState = new ConcurrentHashMap<>();
    private final Map<String, Boolean> thunderstormState = new ConcurrentHashMap<>();

    public ClimateManager(RegionSeasonResolver regionResolver, SeasonClockManager clockManager,
            BiomeTemperatureTable biomeTemperatureTable) {
        this.regionResolver = regionResolver;
        this.clockManager = clockManager;
        this.biomeTemperatureTable = biomeTemperatureTable;
    }

    public double getTemperature(Location location) {

        double base = biomeTemperatureTable.getBaseTemperature(location.getBlock().getBiome());
        Optional<Season> seasonOpt = regionResolver.resolveSeason(location);

        if (seasonOpt.isEmpty()) {
            return base;
        }

        Season season = seasonOpt.get();
        String biomeName = location.getBlock().getBiome().name().toLowerCase(Locale.ROOT);
        double modified = base + season.biomeTemperatureModifiers().getOrDefault(biomeName, 0.0);

        SubSeason subSeason = resolveSubSeason(location, season);
        if (subSeason != null && subSeason.temperatureOverride() != null) {
            modified = subSeason.temperatureOverride();
        }

        String clockKey = regionResolver.resolveClockKey(location);

        if (clockKey != null && isHeatwaveActive(clockKey)) {
            modified += 10.0;
        }

        double variance = season.climate().temperatureVariance();
        if (variance > 0) {
            modified += (random.nextDouble() * 2 - 1) * variance * 0.3;
        }

        return modified;
    }

    private SubSeason resolveSubSeason(Location location, Season season) {

        if (!season.hasSubSeasons()) {
            return null;
        }

        String clockKey = regionResolver.resolveClockKey(location);

        if (clockKey == null) {
            return null;
        }

        return clockManager.get(clockKey)
                .flatMap(clockManager::resolveCurrentSubSeason)
                .orElse(null);
    }

    public boolean isHeatwaveActive(String clockKey) {
        return heatwaveState.getOrDefault(clockKey, false);
    }

    public boolean isThunderstormActive(String clockKey) {
        return thunderstormState.getOrDefault(clockKey, false);
    }

    public void setHeatwaveActive(String clockKey, boolean active) {
        heatwaveState.put(clockKey, active);
    }

    public void setThunderstormActive(String clockKey, boolean active) {
        thunderstormState.put(clockKey, active);
    }

    public Random random() {
        return random;
    }

}
