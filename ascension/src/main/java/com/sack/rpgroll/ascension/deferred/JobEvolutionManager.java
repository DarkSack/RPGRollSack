package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class JobEvolutionManager extends ContentManager<JobEvolution> {

    private final JobEvolutionDefinitionWriter writer;

    public JobEvolutionManager(JavaPlugin ascensionPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ascensionPlugin), "job-evolutions", "evolución de job",
                new JobEvolutionParser());
        this.writer = new JobEvolutionDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(JobEvolution evolution) {
        writer.save(evolution);
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
