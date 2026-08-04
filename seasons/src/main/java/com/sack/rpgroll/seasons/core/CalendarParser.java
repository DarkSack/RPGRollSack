package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

public class CalendarParser implements ContentParser<SeasonCalendar> {

    @Override
    public SeasonCalendar parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new SeasonCalendar(id, config.getString("display-name", id), config.getString("description", ""),
                config.getStringList("seasons"));
    }

}
