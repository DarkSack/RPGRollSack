package com.sack.rpgroll.seasons.core;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CalendarDefinitionWriter {

    private final File folder;

    public CalendarDefinitionWriter(File dataFolder) {
        this.folder = new File(dataFolder, "calendars");
    }

    public void save(SeasonCalendar calendar) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", calendar.id());
        config.set("display-name", calendar.displayName());
        config.set("description", calendar.description());
        config.set("seasons", List.copyOf(calendar.seasonIds()));

        try {
            folder.mkdirs();
            config.save(new File(folder, calendar.id() + ".yml"));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el calendario " + calendar.id(), e);
        }
    }

}
