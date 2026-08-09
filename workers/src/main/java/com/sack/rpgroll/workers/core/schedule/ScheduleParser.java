package com.sack.rpgroll.workers.core.schedule;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScheduleParser implements ContentParser<Schedule> {

    @Override
    public Schedule parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        return new Schedule(id, config.getString("display-name", id), config.getString("description", ""),
                parseEntries(config));
    }

    private List<ScheduleEntry> parseEntries(YamlConfiguration config) {

        List<ScheduleEntry> entries = new ArrayList<>();
        List<?> rawList = config.getList("entries");

        if (rawList == null) {
            return entries;
        }

        for (Object rawEntry : rawList) {

            if (!(rawEntry instanceof java.util.Map<?, ?> rawMap)) {
                continue;
            }

            long startTick = rawMap.get("start-tick") instanceof Number number ? number.longValue() : 0;

            ScheduleActivity activity;
            try {
                activity = ScheduleActivity.valueOf(String.valueOf(rawMap.get("activity")).toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                continue;
            }

            entries.add(new ScheduleEntry(startTick, activity));
        }

        return entries;
    }

}
