package com.sack.rpgroll.seasons.runtime;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

/**
 * Persiste TODOS los relojes de {@link SeasonClockManager} en un único
 * archivo plugins/RPGRoll-Seasons/state.yml — a diferencia del resto de
 * los estados por-jugador de otros addons, acá el estado es global al
 * servidor (por mundo/región, no por jugador).
 */
public class SeasonStateStore {

    private final Plugin plugin;
    private final File file;

    public SeasonStateStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "state.yml");
    }

    public void loadInto(SeasonClockManager clockManager) {

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        var section = config.getConfigurationSection("clocks");

        if (section == null) {
            return;
        }

        for (String clockKey : section.getKeys(false)) {

            String calendarId = section.getString(clockKey + ".calendar");

            if (calendarId == null) {
                continue;
            }

            CalendarState state = new CalendarState(clockKey, calendarId);
            state.restoreExact(
                    section.getInt(clockKey + ".season-index", 0),
                    section.getInt(clockKey + ".sub-season-index", -1),
                    section.getLong(clockKey + ".elapsed-ticks", 0),
                    section.getInt(clockKey + ".year", 1),
                    section.getLong(clockKey + ".last-world-event-day-roll", -1));
            clockManager.restore(state);
        }
    }

    public void save(SeasonClockManager clockManager) {

        YamlConfiguration config = new YamlConfiguration();

        for (var entry : clockManager.allClocks().entrySet()) {

            String clockKey = entry.getKey();
            CalendarState state = entry.getValue();

            config.set("clocks." + clockKey + ".calendar", state.calendarId());
            config.set("clocks." + clockKey + ".season-index", state.seasonIndex());
            config.set("clocks." + clockKey + ".sub-season-index", state.subSeasonIndex());
            config.set("clocks." + clockKey + ".elapsed-ticks", state.elapsedTicks());
            config.set("clocks." + clockKey + ".year", state.year());
            config.set("clocks." + clockKey + ".last-world-event-day-roll", state.lastWorldEventDayRoll());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando el estado de estaciones: " + e.getMessage());
        }
    }

}
