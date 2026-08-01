package com.sack.rpgroll.gameplay.job;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.yaml.YamlLoader;
import com.sack.rpgroll.common.content.ContentManager;

public class JobManager extends ContentManager<Job> {

    public JobManager(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "jobs", "trabajo", new JobParser(plugin));
    }

}