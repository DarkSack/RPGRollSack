package com.sack.rpgroll.ascension.reward;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Lee un bloque {@code rewards:}. Ver {@link Rewards} para los campos. */
public final class RewardsParser {

    private RewardsParser() {
    }

    public static Rewards parse(ConfigurationSection section) {

        if (section == null) {
            return Rewards.none();
        }

        Map<String, Integer> reputation = new HashMap<>();
        ConfigurationSection reputationSection = section.getConfigurationSection("reputation");
        if (reputationSection != null) {
            for (String faction : reputationSection.getKeys(false)) {
                reputation.put(faction, reputationSection.getInt(faction));
            }
        }

        Map<String, Double> stats = new HashMap<>();
        ConfigurationSection statsSection = section.getConfigurationSection("stats");
        if (statsSection != null) {
            for (String stat : statsSection.getKeys(false)) {
                stats.put(stat.toLowerCase(Locale.ROOT), statsSection.getDouble(stat));
            }
        }

        return new Rewards(
                section.getDouble("money", 0),
                section.getInt("experience", 0),
                section.getInt("talent-points", 0),
                section.getString("title"),
                reputation,
                stats,
                section.getStringList("items"),
                section.getStringList("commands"),
                section.getString("message"),
                section.getBoolean("broadcast", false));
    }

}
