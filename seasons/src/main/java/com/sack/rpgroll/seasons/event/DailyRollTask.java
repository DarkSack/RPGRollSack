package com.sack.rpgroll.seasons.event;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.WorldEventManager;
import com.sack.rpgroll.seasons.integration.MobsIntegration;
import com.sack.rpgroll.seasons.runtime.CalendarState;
import com.sack.rpgroll.seasons.runtime.RegionSeasonResolver;
import com.sack.rpgroll.seasons.runtime.SeasonClockManager;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Random;

/**
 * Una vez por día de Minecraft (según {@code World#getFullTime()}, sin
 * relación con la unidad de duración configurada en las estaciones) por
 * cada mundo con jugadores, sortea si dispara un evento mundial elegible
 * de la estación activa y si aparece su jefe exclusivo.
 */
public class DailyRollTask implements Runnable {

    private static final long TICKS_PER_DAY = 24000L;
    private static final double EXCLUSIVE_BOSS_DAILY_CHANCE = 0.15;

    private final SeasonClockManager clockManager;
    private final WorldEventManager worldEventManager;
    private final WorldEventEngine worldEventEngine;
    private final String defaultCalendarId;
    private final LangManager lang;
    private final Random random = new Random();

    public DailyRollTask(SeasonClockManager clockManager, WorldEventManager worldEventManager,
            WorldEventEngine worldEventEngine, String defaultCalendarId, LangManager lang) {
        this.clockManager = clockManager;
        this.worldEventManager = worldEventManager;
        this.worldEventEngine = worldEventEngine;
        this.defaultCalendarId = defaultCalendarId;
        this.lang = lang;
    }

    @Override
    public void run() {

        for (World world : Bukkit.getWorlds()) {

            if (world.getPlayers().isEmpty()) {
                continue;
            }

            String clockKey = RegionSeasonResolver.worldClockKey(world.getName());
            CalendarState state = clockManager.getOrCreate(clockKey, defaultCalendarId);
            long today = world.getFullTime() / TICKS_PER_DAY;

            if (state.lastWorldEventDayRoll() == today) {
                continue;
            }

            state.setLastWorldEventDayRoll(today);
            rollForWorld(state, world);
        }
    }

    private void rollForWorld(CalendarState state, World world) {

        Season season = clockManager.resolveCurrentSeason(state).orElse(null);

        if (season == null) {
            return;
        }

        if (!season.worldEventIds().isEmpty() && random.nextDouble() < season.worldEventDailyChance()) {

            String eventId = season.worldEventIds().get(random.nextInt(season.worldEventIds().size()));

            worldEventManager.get(eventId).ifPresent(event -> worldEventEngine.trigger(event, world));
        }

        if (season.hasExclusiveBoss() && MobsIntegration.isAvailable()
                && random.nextDouble() < EXCLUSIVE_BOSS_DAILY_CHANCE) {
            spawnExclusiveBoss(season, world);
        }
    }

    private void spawnExclusiveBoss(Season season, World world) {

        var players = world.getPlayers();

        if (players.isEmpty()) {
            return;
        }

        var target = players.get(random.nextInt(players.size()));

        MobsIntegration.spawnMob(season.exclusiveBossId(), target.getLocation()).ifPresent(entity -> {

            Component announce = lang.component("event.boss_announce", "season", season.displayName());

            world.getPlayers().forEach(player -> player.sendMessage(announce));
        });
    }

}
