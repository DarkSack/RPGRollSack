package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class CalendarManager extends ContentManager<SeasonCalendar> {

    private final CalendarDefinitionWriter writer;

    public CalendarManager(JavaPlugin seasonsPlugin) {
        super(owningPlugin(), new YamlLoader(seasonsPlugin), "calendars", "calendario", new CalendarParser());
        this.writer = new CalendarDefinitionWriter(seasonsPlugin.getDataFolder());
    }

    public void save(SeasonCalendar calendar) {
        writer.save(calendar);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(CalendarManager.class);
    }

}
