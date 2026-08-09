package com.sack.rpgroll.workers.core.schedule;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ScheduleManager extends ContentManager<Schedule> {

    private final ScheduleDefinitionWriter writer;

    public ScheduleManager(JavaPlugin workersPlugin) {
        super(resolveCoreInstance(), new YamlLoader(workersPlugin), "schedules", "horario", new ScheduleParser());
        this.writer = new ScheduleDefinitionWriter(workersPlugin.getDataFolder());
    }

    public void save(Schedule schedule) {
        writer.save(schedule);
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
