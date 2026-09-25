package com.sack.rpgroll.workers.core.event;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class WorkerEventManager extends ContentManager<WorkerEventDefinition> {

    private final WorkerEventDefinitionWriter writer;

    public WorkerEventManager(JavaPlugin workersPlugin) {
        super(owningPlugin(), new YamlLoader(workersPlugin), "worker-events", "evento", new WorkerEventParser());
        this.writer = new WorkerEventDefinitionWriter(workersPlugin.getDataFolder());
    }

    public void save(WorkerEventDefinition event) {
        writer.save(event);
        reload();
    }

    public List<WorkerEventDefinition> getByType(WorkerEventType type) {
        return getAll().stream().filter(event -> event.type() == type).toList();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(WorkerEventManager.class);
    }

}
