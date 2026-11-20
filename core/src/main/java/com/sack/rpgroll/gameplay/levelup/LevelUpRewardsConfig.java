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

    private static final int MAX_LEVEL = com.sack.rpgroll.player.progression.PlayerProgression.MAX_LEVEL;

    private final RPGRoll plugin;
    private final YamlLoader yamlLoader;
    private final Map<Integer, LevelUpRewards> rewards;
    /** XP de los niveles que no la fijan; se rehace en cada {@link #load()}. */
    private ExpCurve curve = new ExpCurve(new TreeMap<>());

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
        curve = new ExpCurve(new TreeMap<>());

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

        // Primero los niveles que fijan exp_required: con ellos se calcula la XP de los demás.
        TreeMap<Integer, Integer> anchors = new TreeMap<>();
        for (String levelKey : rewardsSection.getKeys(false)) {
            ConfigurationSection s = rewardsSection.getConfigurationSection(levelKey);
            if (s != null && s.isInt("exp_required")) {
                try {
                    anchors.put(Integer.parseInt(levelKey), s.getInt("exp_required"));
                } catch (NumberFormatException ignored) {
                    // Se avisa abajo.
                }
            }
        }
        curve = new ExpCurve(anchors);

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

        int expRequired = section.getInt("exp_required", curve.expRequired(level));
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
        return ExpCurve.formula(level);
    }

    /** XP acumulada que pide {@code level}: la de levelup-rewards.yml o la de la curva. El nivel 1 pide 0. */
    public int expRequired(int level) {
        if (level <= 1) {
            return 0;
        }
        return getRewards(level).map(LevelUpRewards::experienceRequired).orElse(Integer.MAX_VALUE);
    }

    /** XP que le falta para el siguiente nivel (0 en el máximo). */
    public int expToNext(int level, int experience) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return Math.max(0, expRequired(level + 1) - experience);
    }

    /** Avance dentro del nivel, de 0 a 100 (100 en el máximo). */
    public int progressPercent(int level, int experience) {
        if (level >= MAX_LEVEL) {
            return 100;
        }
        return ExpCurve.percent(experience, expRequired(level), expRequired(level + 1));
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
        int expRequired = curve.expRequired(level);
        int statPoints = 2;
        int healthBonus = 5 * level;
        int manaBonus = 3 * level;

        return new LevelUpRewards(level, expRequired, statPoints, healthBonus, manaBonus, List.of(), List.of());
    }

}
