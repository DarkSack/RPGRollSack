package com.sack.rpgroll.workers.core.schedule;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class ScheduleManager extends ContentManager<Schedule> {

    private final ScheduleDefinitionWriter writer;

    public ScheduleManager(JavaPlugin workersPlugin) {
        super(owningPlugin(), new YamlLoader(workersPlugin), "schedules", "horario", new ScheduleParser());
        this.writer = new ScheduleDefinitionWriter(workersPlugin.getDataFolder());
    }

    public void save(Schedule schedule) {
        writer.save(schedule);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(ScheduleManager.class);
    }

}
