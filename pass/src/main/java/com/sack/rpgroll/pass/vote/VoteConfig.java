package com.sack.rpgroll.pass.vote;

import com.sack.rpgroll.pass.reward.Reward;
import com.sack.rpgroll.pass.reward.RewardParser;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/** votes.yml: páginas de votación, recompensa por voto y premios por racha de días votando. */
public record VoteConfig(List<Site> sites, List<Reward> rewards, int passXp, Map<Integer, List<Reward>> streaks,
        String broadcast) {

    public record Site(String name, String url) {
    }

    public static VoteConfig parse(ConfigurationSection config, Consumer<String> warn) {

        List<Site> sites = new ArrayList<>();
        for (var entry : config.getMapList("sites")) {
            Object name = entry.get("name");
            Object url = entry.get("url");
            if (name != null && url != null) {
                sites.add(new Site(name.toString(), url.toString()));
            }
        }

        Map<Integer, List<Reward>> streaks = new TreeMap<>();
        ConfigurationSection streakSection = config.getConfigurationSection("streak");
        if (streakSection != null) {
            for (String key : streakSection.getKeys(false)) {
                try {
                    int days = Integer.parseInt(key);
                    streaks.put(days, RewardParser.parseAll(streakSection.getStringList(key),
                            "racha de votos " + days, warn));
                } catch (NumberFormatException e) {
                    warn.accept("votes.yml: la racha '" + key + "' no es un número de días.");
                }
            }
        }

        return new VoteConfig(List.copyOf(sites),
                RewardParser.parseAll(config.getStringList("rewards"), "votos", warn),
                Math.max(0, config.getInt("pass-xp", 0)), Map.copyOf(streaks),
                config.getString("broadcast", ""));
    }

}
