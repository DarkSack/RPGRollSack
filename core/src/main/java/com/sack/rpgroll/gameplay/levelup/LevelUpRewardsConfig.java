package com.sack.rpgroll.gameplay.levelup;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.yaml.YamlLoader;

import java.util.*;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Carga y gestiona las recompensas de level up desde YAML.
 */
public class LevelUpRewardsConfig {

    private final RPGRoll plugin;
    private final YamlLoader yamlLoader;
    private final Map<Integer, LevelUpRewards> rewards;

    public LevelUpRewardsConfig(RPGRoll plugin, YamlLoader yamlLoader) {
        this.plugin = plugin;
        this.yamlLoader = yamlLoader;
        this.rewards = new HashMap<>();
    }

    /**
     * Carga las recompensas desde levelup-rewards.yml.
     */
    public void load() {
        rewards.clear();

        YamlConfiguration config = yamlLoader.loadConfig("levelup-rewards.yml");

        if (config == null) {
            plugin.getLogger().warning("No se pudo cargar levelup-rewards.yml");
            setDefaults();
            return;
        }

        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");

        if (rewardsSection == null) {
            plugin.getLogger().warning("No se encontró sección 'rewards' en levelup-rewards.yml");
            setDefaults();
            return;
        }

        for (String levelKey : rewardsSection.getKeys(false)) {

            int level;
            try {
                level = Integer.parseInt(levelKey);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Nivel inválido: " + levelKey);
                continue;
            }

            ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(levelKey);

            if (rewardSection == null) {
                plugin.getLogger().warning("La recompensa del nivel " + level + " está vacía.");
                continue;
            }

            try {
                rewards.put(level, parseReward(level, rewardSection));
            } catch (Exception e) {
                plugin.getLogger().warning("Error al cargar recompensa del nivel " + level);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("✔ " + rewards.size() + " recompensas de level up cargadas.");
    }

    /**
     * Establece valores por defecto si no hay configuración.
     */
    private void setDefaults() {
        // Puntos de stat por nivel
        for (int level = 1; level <= 100; level++) {
            int expRequired = calculateExpRequired(level);
            int statPoints = 2; // 2 puntos por nivel
            int healthBonus = 5 * level; // 5 HP por nivel
            int manaBonus = 3 * level; // 3 Maná por nivel

            LevelUpRewards reward = new LevelUpRewards(
                    level,
                    expRequired,
                    statPoints,
                    healthBonus,
                    manaBonus,
                    List.of(),
                    List.of());

            rewards.put(level, reward);
        }
    }

    /**
     * Parsea una recompensa desde el YAML.
     */
    private LevelUpRewards parseReward(int level, ConfigurationSection section) {

        int expRequired = section.getInt("exp_required", calculateExpRequired(level));
        int statPoints = section.getInt("stat_points", 2);
        int healthBonus = section.getInt("health_bonus", 5 * level);
        int manaBonus = section.getInt("mana_bonus", 3 * level);

        List<String> unlockedSkills = section.getStringList("unlocked_skills");
        List<String> unlockedTraits = section.getStringList("unlocked_traits");

        return new LevelUpRewards(
                level,
                expRequired,
                statPoints,
                healthBonus,
                manaBonus,
                unlockedSkills,
                unlockedTraits);
    }

    /**
     * Calcula la experiencia requerida para un nivel (fórmula exponencial).
     */
    private int calculateExpRequired(int level) {
        int baseExp = 100;
        double multiplier = 1.5;
        return (int) (baseExp * Math.pow(level - 1, multiplier));
    }

    /**
     * Obtiene las recompensas para un nivel específico.
     */
    public Optional<LevelUpRewards> getRewards(int level) {
        return Optional.ofNullable(rewards.getOrDefault(level, createDefault(level)));
    }

    /**
     * Crea una recompensa por defecto si no existe.
     */
    private LevelUpRewards createDefault(int level) {
        int expRequired = calculateExpRequired(level);
        int statPoints = 2;
        int healthBonus = 5 * level;
        int manaBonus = 3 * level;

        return new LevelUpRewards(level, expRequired, statPoints, healthBonus, manaBonus, List.of(), List.of());
    }

}
