package com.sack.rpgroll.gameplay.job;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.yaml.YamlLoader;
import com.sack.rpgroll.common.content.ContentManager;

import java.io.File;

public class JobManager extends ContentManager<Job> {

    private final JobDefinitionWriter writer;

    public JobManager(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "jobs", "trabajo", new JobParser(plugin));
        this.writer = new JobDefinitionWriter(new File(plugin.getDataFolder(), "jobs"), plugin.getLogger());
    }

    /** Persiste el trabajo a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(Job job) {
        writer.save(job);
        reload();
    }

}