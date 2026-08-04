package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CalendarManager extends ContentManager<SeasonCalendar> {

    private final CalendarDefinitionWriter writer;

    public CalendarManager(JavaPlugin seasonsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(seasonsPlugin), "calendars", "calendario", new CalendarParser());
        this.writer = new CalendarDefinitionWriter(seasonsPlugin.getDataFolder());
    }

    public void save(SeasonCalendar calendar) {
        writer.save(calendar);
        reload();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
