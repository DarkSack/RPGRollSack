package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Locale;

public class JobEvolutionParser implements ContentParser<JobEvolution> {

    @Override
    public JobEvolution parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String baseJob = config.getString("base-job");
        if (baseJob == null || baseJob.isBlank()) {
            throw new IllegalArgumentException("evolución de job '" + id + "' sin campo obligatorio 'base-job'");
        }

        return new JobEvolution(id, baseJob.toLowerCase(Locale.ROOT), config.getString("display-name", id),
                config.getInt("required-job-level", 0), config.getStringList("unlocked-recipes"),
                config.getStringList("unlocked-tools"), config.getStringList("unlocked-quests"));
    }

}
