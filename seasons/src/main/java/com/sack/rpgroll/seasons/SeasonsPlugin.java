package com.sack.rpgroll.seasons;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.common.resource.ResourceFile;
import com.sack.rpgroll.seasons.api.SeasonsAPI;
import com.sack.rpgroll.seasons.climate.BiomeTemperatureTable;
import com.sack.rpgroll.seasons.climate.ClimateManager;
import com.sack.rpgroll.seasons.climate.WeatherTickTask;
import com.sack.rpgroll.seasons.command.SeasonsAdminCommand;
import com.sack.rpgroll.seasons.command.SeasonsCommand;
import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SeasonRegionManager;
import com.sack.rpgroll.seasons.core.WorldEventManager;
import com.sack.rpgroll.seasons.event.DailyRollTask;
import com.sack.rpgroll.seasons.event.WorldEventEngine;
import com.sack.rpgroll.seasons.gui.ChatPromptManager;
import com.sack.rpgroll.seasons.integration.SeasonMobSpawnTask;
import com.sack.rpgroll.seasons.runtime.RegionSeasonResolver;
import com.sack.rpgroll.seasons.runtime.SeasonClockManager;
import com.sack.rpgroll.seasons.runtime.SeasonStateStore;
import com.sack.rpgroll.seasons.vegetation.VegetationTask;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class SeasonsPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("calendars", "seasons", "events", "regions");
    private static final long CLOCK_TICK_INTERVAL = 1200L;
    private static final long WEATHER_TICK_INTERVAL = 6000L;
    private static final long VEGETATION_TICK_INTERVAL = 100L;
    private static final long MOB_SPAWN_TICK_INTERVAL = 600L;
    private static final long DAILY_ROLL_TICK_INTERVAL = 1200L;
    private static final long AUTOSAVE_TICK_INTERVAL = 6000L;

    /** Calendario usado por defecto para cualquier mundo sin región que lo pise — ver calendars/default_calendar.yml. */
    private static final String DEFAULT_CALENDAR_ID = "default_calendar";

    private CalendarManager calendarManager;
    private SeasonManager seasonManager;
    private WorldEventManager worldEventManager;
    private SeasonRegionManager regionManager;
    private SeasonClockManager clockManager;
    private SeasonStateStore stateStore;
    private LangManager langManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);
        new ResourceCopier(this).copyFiles(
                List.of(new ResourceFile("biome-temperatures.yml", "biome-temperatures.yml", true)));

        calendarManager = new CalendarManager(this);
        calendarManager.initialize();

        seasonManager = new SeasonManager(this);
        seasonManager.initialize();

        worldEventManager = new WorldEventManager(this);
        worldEventManager.initialize();

        regionManager = new SeasonRegionManager(this);
        regionManager.initialize();

        clockManager = new SeasonClockManager(seasonManager, calendarManager);
        stateStore = new SeasonStateStore(this);
        stateStore.loadInto(clockManager);

        RegionSeasonResolver regionResolver = new RegionSeasonResolver(regionManager, seasonManager, clockManager,
                DEFAULT_CALENDAR_ID);

        BiomeTemperatureTable biomeTemperatureTable = new BiomeTemperatureTable(this);
        ClimateManager climateManager = new ClimateManager(regionResolver, clockManager, biomeTemperatureTable);

        WorldEventEngine worldEventEngine = new WorldEventEngine(this, langManager);

        SeasonsAPI.init(seasonManager, calendarManager, worldEventManager, regionManager, clockManager,
                regionResolver, climateManager, worldEventEngine, DEFAULT_CALENDAR_ID);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        Bukkit.getScheduler().runTaskTimer(this, () -> clockManager.tick(CLOCK_TICK_INTERVAL), CLOCK_TICK_INTERVAL,
                CLOCK_TICK_INTERVAL);
        new WeatherTickTask(clockManager, climateManager, DEFAULT_CALENDAR_ID)
                .runTaskTimer(this, WEATHER_TICK_INTERVAL, WEATHER_TICK_INTERVAL);
        Bukkit.getScheduler().runTaskTimer(this, new VegetationTask(regionResolver, climateManager),
                VEGETATION_TICK_INTERVAL, VEGETATION_TICK_INTERVAL);
        Bukkit.getScheduler().runTaskTimer(this, new SeasonMobSpawnTask(regionResolver), MOB_SPAWN_TICK_INTERVAL,
                MOB_SPAWN_TICK_INTERVAL);
        Bukkit.getScheduler().runTaskTimer(this,
                new DailyRollTask(clockManager, worldEventManager, worldEventEngine, DEFAULT_CALENDAR_ID, langManager),
                DAILY_ROLL_TICK_INTERVAL, DAILY_ROLL_TICK_INTERVAL);
        Bukkit.getScheduler().runTaskTimer(this, () -> stateStore.save(clockManager), AUTOSAVE_TICK_INTERVAL,
                AUTOSAVE_TICK_INTERVAL);

        var adminCommand = getCommand("seasonsadmin");
        if (adminCommand == null) {
            getLogger().severe("✘ El comando 'seasonsadmin' no está declarado en plugin.yml");
        } else {
            var seasonsAdminCommand = new SeasonsAdminCommand(this, calendarManager, seasonManager, worldEventManager,
                    regionManager, worldEventEngine, SeasonsAPI.get(), chatPromptManager);
            adminCommand.setExecutor(seasonsAdminCommand);
            adminCommand.setTabCompleter(seasonsAdminCommand);
        }

        var playerCommand = getCommand("seasons");
        if (playerCommand == null) {
            getLogger().severe("✘ El comando 'seasons' no está declarado en plugin.yml");
        } else {
            var seasonsCommand = new SeasonsCommand(langManager);
            playerCommand.setExecutor(seasonsCommand);
            playerCommand.setTabCompleter(seasonsCommand);
        }

        getLogger().info("✔ RPGRoll-Seasons habilitado. " + calendarManager.count() + " calendario(s), "
                + seasonManager.count() + " estación(es), " + worldEventManager.count() + " evento(s), "
                + regionManager.count() + " región(es).");
    }

    @Override
    public void onDisable() {
        if (stateStore != null && clockManager != null) {
            stateStore.save(clockManager);
        }
    }

    public CalendarManager getCalendarManager() {
        return calendarManager;
    }

    public SeasonManager getSeasonManager() {
        return seasonManager;
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

    public LangManager getLangManager() {
        return langManager;
    }

}
