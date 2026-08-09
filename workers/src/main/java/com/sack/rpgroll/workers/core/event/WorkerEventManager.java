package com.sack.rpgroll.workers.core.event;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class WorkerEventManager extends ContentManager<WorkerEventDefinition> {

    private final WorkerEventDefinitionWriter writer;

    public WorkerEventManager(JavaPlugin workersPlugin) {
        super(resolveCoreInstance(), new YamlLoader(workersPlugin), "worker-events", "evento", new WorkerEventParser());
        this.writer = new WorkerEventDefinitionWriter(workersPlugin.getDataFolder());
    }

    public void save(WorkerEventDefinition event) {
        writer.save(event);
        reload();
    }

    public List<WorkerEventDefinition> getByType(WorkerEventType type) {
        return getAll().stream().filter(event -> event.type() == type).toList();
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
