package com.sack.rpgroll.seasons.runtime;

import com.sack.rpgroll.seasons.core.CalendarManager;
import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.SeasonCalendar;
import com.sack.rpgroll.seasons.core.SeasonManager;
import com.sack.rpgroll.seasons.core.SubSeason;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de todos los relojes de calendario activos, identificados por
 * clave libre ({@code "world:<nombre>"} para el calendario normal de un
 * mundo, {@code "region:<id>"} para una {@code SeasonRegion} con {@code
 * PINNED_CALENDAR}). Avanza todos los relojes juntos en cada {@link #tick}
 * — cada uno con su propio calendario/estación/año independientes.
 */
public class SeasonClockManager {

    private final SeasonManager seasonManager;
    private final CalendarManager calendarManager;
    private final Map<String, CalendarState> clocks = new ConcurrentHashMap<>();

    public SeasonClockManager(SeasonManager seasonManager, CalendarManager calendarManager) {
        this.seasonManager = seasonManager;
        this.calendarManager = calendarManager;
    }

    public CalendarState getOrCreate(String clockKey, String defaultCalendarId) {
        return clocks.computeIfAbsent(clockKey, key -> new CalendarState(key, defaultCalendarId));
    }

    public Optional<CalendarState> get(String clockKey) {
        return Optional.ofNullable(clocks.get(clockKey));
    }

    public Map<String, CalendarState> allClocks() {
        return clocks;
    }

    public void restore(CalendarState state) {
        clocks.put(state.clockKey(), state);
    }

    /** Avanza todos los relojes {@code deltaTicks} — llamado periódicamente por SeasonTickTask. */
    public void tick(long deltaTicks) {
        for (CalendarState state : clocks.values()) {
            tickOne(state, deltaTicks);
        }
    }

    private void tickOne(CalendarState state, long deltaTicks) {

        Optional<SeasonCalendar> calendarOpt = calendarManager.get(state.calendarId());

        if (calendarOpt.isEmpty()) {
            return;
        }

        List<String> seasonIds = calendarOpt.get().seasonIds();

        if (seasonIds.isEmpty()) {
            return;
        }

        state.addElapsedTicks(deltaTicks);

        int guard = 0;

        while (guard++ < 64) {

            int seasonIndex = Math.floorMod(state.seasonIndex(), seasonIds.size());
            Optional<Season> seasonOpt = seasonManager.get(seasonIds.get(seasonIndex));

            if (seasonOpt.isEmpty()) {
                return;
            }

            Season season = seasonOpt.get();

            if (season.hasSubSeasons()) {

                if (state.subSeasonIndex() < 0) {
                    state.advanceSubSeason();
                    continue;
                }

                int subIndex = Math.min(state.subSeasonIndex(), season.subSeasons().size() - 1);
                SubSeason sub = season.subSeasons().get(subIndex);

                if (state.elapsedTicks() < sub.durationTicks()) {
                    return;
                }

                if (subIndex + 1 < season.subSeasons().size()) {
                    state.advanceSubSeason();
                } else {
                    state.advanceSeason(seasonIds.size());
                }

                continue;
            }

            if (state.elapsedTicks() < season.durationTicks()) {
                return;
            }

            state.advanceSeason(seasonIds.size());
        }
    }

    public Optional<Season> resolveCurrentSeason(String clockKey) {

        CalendarState state = clocks.get(clockKey);

        if (state == null) {
            return Optional.empty();
        }

        return resolveCurrentSeason(state);
    }

    public Optional<Season> resolveCurrentSeason(CalendarState state) {

        return calendarManager.get(state.calendarId())
                .filter(calendar -> !calendar.seasonIds().isEmpty())
                .flatMap(calendar -> {
                    int index = Math.floorMod(state.seasonIndex(), calendar.seasonIds().size());
                    return seasonManager.get(calendar.seasonIds().get(index));
                });
    }

    public Optional<SubSeason> resolveCurrentSubSeason(CalendarState state) {

        return resolveCurrentSeason(state).filter(Season::hasSubSeasons).map(season -> {
            int index = Math.max(0, Math.min(state.subSeasonIndex(), season.subSeasons().size() - 1));
            return season.subSeasons().get(index);
        });
    }

}
