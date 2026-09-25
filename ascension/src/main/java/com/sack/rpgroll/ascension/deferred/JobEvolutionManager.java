package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

public class JobEvolutionManager extends ContentManager<JobEvolution> {

    private final JobEvolutionDefinitionWriter writer;

    public JobEvolutionManager(JavaPlugin ascensionPlugin) {
        super(owningPlugin(), new YamlLoader(ascensionPlugin), "job-evolutions", "evolución de job",
                new JobEvolutionParser());
        this.writer = new JobEvolutionDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(JobEvolution evolution) {
        writer.save(evolution);
        reload();
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(JobEvolutionManager.class);
    }

}
