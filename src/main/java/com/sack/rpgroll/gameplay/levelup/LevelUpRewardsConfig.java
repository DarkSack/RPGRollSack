package com.sack.rpgroll.gameplay.levelup;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.loader.YamlLoader;

import java.util.*;

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

        Map<String, Object> config = yamlLoader.load("levelup-rewards.yml");

        if (config == null || config.isEmpty()) {
            plugin.getLogger().warning("No se pudo cargar levelup-rewards.yml");
            setDefaults();
            return;
        }

        Map<String, Object> rewardsData = (Map<String, Object>) config.get("rewards");

        if (rewardsData == null) {
            plugin.getLogger().warning("No se encontró sección 'rewards' en levelup-rewards.yml");
            setDefaults();
            return;
        }

        for (Map.Entry<String, Object> entry : rewardsData.entrySet()) {
            int level;
            try {
                level = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException e) {
                continue;
            }

            Map<String, Object> rewardData = (Map<String, Object>) entry.getValue();

            try {
                LevelUpRewards reward = parseReward(level, rewardData);
                rewards.put(level, reward);
            } catch (Exception e) {
                plugin.getLogger().warning("Error al cargar recompensa del nivel: " + level);
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
    private LevelUpRewards parseReward(int level, Map<String, Object> data) {
        int expRequired = ((Number) data.getOrDefault("exp_required", calculateExpRequired(level))).intValue();
        int statPoints = ((Number) data.getOrDefault("stat_points", 2)).intValue();
        int healthBonus = ((Number) data.getOrDefault("health_bonus", 5 * level)).intValue();
        int manaBonus = ((Number) data.getOrDefault("mana_bonus", 3 * level)).intValue();

        List<String> unlockedSkills = (List<String>) data.getOrDefault("unlocked_skills", List.of());
        List<String> unlockedTraits = (List<String>) data.getOrDefault("unlocked_traits", List.of());

        return new LevelUpRewards(level, expRequired, statPoints, healthBonus, manaBonus, unlockedSkills,
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
