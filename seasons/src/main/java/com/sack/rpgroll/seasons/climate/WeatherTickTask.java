package com.sack.rpgroll.seasons.climate;

import com.sack.rpgroll.seasons.core.ClimateProfile;
import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.runtime.RegionSeasonResolver;
import com.sack.rpgroll.seasons.runtime.SeasonClockManager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * Re-sortea el clima de cada mundo con jugadores online, cada {@code
 * intervalTicks} — la vegetación/nieve/lluvia siguen siendo vanilla
 * (World#setStorm/setThundering), solo que ahora las probabilidades las
 * define la estación activa en vez del generador aleatorio de Minecraft.
 * Se salta mundos vacíos para no generar carga innecesaria.
 */
public class WeatherTickTask extends BukkitRunnable {

    private final SeasonClockManager clockManager;
    private final ClimateManager climateManager;
    private final String defaultCalendarId;
    private final Random random = new Random();

    public WeatherTickTask(SeasonClockManager clockManager, ClimateManager climateManager, String defaultCalendarId) {
        this.clockManager = clockManager;
        this.climateManager = climateManager;
        this.defaultCalendarId = defaultCalendarId;
    }

    @Override
    public void run() {

        for (World world : Bukkit.getWorlds()) {

            if (world.getPlayers().isEmpty()) {
                continue;
            }

            String clockKey = RegionSeasonResolver.worldClockKey(world.getName());
            var state = clockManager.getOrCreate(clockKey, defaultCalendarId);
            Season season = clockManager.resolveCurrentSeason(state).orElse(null);

            if (season == null) {
                continue;
            }

            applyClimate(world, clockKey, season.climate());
        }
    }

    private void applyClimate(World world, String clockKey, ClimateProfile climate) {

        boolean storm = random.nextDouble() < Math.max(climate.rainChance(), climate.stormChance());
        boolean thunder = storm && random.nextDouble() < climate.thunderstormChance();
        boolean heatwave = !storm && random.nextDouble() < climate.heatwaveChance();

        world.setStorm(storm);
        world.setThundering(thunder);

        climateManager.setHeatwaveActive(clockKey, heatwave);
        climateManager.setThunderstormActive(clockKey, thunder);
    }

}
