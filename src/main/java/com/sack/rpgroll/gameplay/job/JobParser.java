package com.sack.rpgroll.gameplay.job;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.content.ContentParser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobParser implements ContentParser<Job> {

    private final RPGRoll plugin;

    public JobParser(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public Job parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String description = config.getString("description", "");
        String icon = config.getString("icon", "");
        List<String> lore = config.getStringList("lore");
        int maxLevel = config.getInt("max-level", 50);
        int expBase = config.getInt("exp-base", 100);
        double expMultiplier = config.getDouble("exp-multiplier", 1.4);

        Map<String, JobReward> rewards = parseRewards(config, id);
        double newBiomeMoney = config.getDouble("new-biome-money", 0.0);
        int newBiomeExperience = config.getInt("new-biome-experience", 0);
        int distanceBlocks = config.getInt("distance-blocks", 0);
        double distanceMoney = config.getDouble("distance-money", 0.0);
        int distanceExperience = config.getInt("distance-experience", 0);

        return new Job(id, displayName, description, icon, lore, maxLevel, expBase, expMultiplier, rewards,
                newBiomeMoney, newBiomeExperience, distanceBlocks, distanceMoney, distanceExperience);
    }

    private Map<String, JobReward> parseRewards(YamlConfiguration config, String jobId) {

        Map<String, JobReward> rewards = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("rewards");

        boolean isExplorationJob = config.contains("new-biome-money") || config.contains("distance-blocks");

        if (section == null) {
            if (!isExplorationJob) {
                plugin.getLogger().warning("✘ Trabajo '" + jobId + "' sin sección 'rewards'.");
            }
            return rewards;
        }
        for (String target : section.getKeys(false)) {

            ConfigurationSection rewardSection = section.getConfigurationSection(target);

            if (rewardSection == null) {
                plugin.getLogger().warning(
                        "✘ Trabajo '" + jobId + "': recompensa inválida para '" + target + "', ignorada.");
                continue;
            }

            double money = rewardSection.getDouble("money", 0.0);
            int experience = rewardSection.getInt("experience", 0);

            rewards.put(target.toUpperCase(), new JobReward(money, experience));
        }

        return rewards;
    }

}