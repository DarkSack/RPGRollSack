package com.sack.rpgroll.pass.daily;

import com.sack.rpgroll.pass.reward.Reward;
import com.sack.rpgroll.pass.reward.RewardParser;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * daily.yml: un ciclo de días que avanza con la racha, un extra por rango
 * (gana el primero de la lista que el jugador tenga) y puntos de pase.
 */
public record DailyConfig(boolean resetIfMissed, int passXp, List<List<Reward>> days, List<RankBonus> bonuses) {

    /** Extra diario para quien tenga {@code permission}. */
    public record RankBonus(String permission, String name, List<Reward> rewards) {
    }

    public static DailyConfig parse(ConfigurationSection config, Consumer<String> warn) {

        List<List<Reward>> days = new ArrayList<>();
        ConfigurationSection daySection = config.getConfigurationSection("days");

        if (daySection != null) {
            for (int day = 1; daySection.contains(String.valueOf(day)); day++) {
                days.add(RewardParser.parseAll(daySection.getStringList(String.valueOf(day)), "diario día " + day,
                        warn));
            }
        }

        if (days.isEmpty()) {
            warn.accept("daily.yml no tiene días (days: 1, 2, 3...): la recompensa diaria queda vacía.");
        }

        List<RankBonus> bonuses = new ArrayList<>();
        for (var entry : config.getMapList("rank-bonus")) {

            Object permission = entry.get("permission");
            Object rewards = entry.get("rewards");

            if (!(permission instanceof String node) || !(rewards instanceof List<?> list)) {
                warn.accept("daily.yml: cada rank-bonus necesita permission y rewards.");
                continue;
            }

            Object name = entry.get("name");
            bonuses.add(new RankBonus(node, name == null ? node : name.toString(),
                    RewardParser.parseAll(list.stream().map(String::valueOf).toList(), "bonus " + node, warn)));
        }

        return new DailyConfig(config.getBoolean("reset-streak-if-missed", true),
                Math.max(0, config.getInt("pass-xp", 0)), List.copyOf(days), List.copyOf(bonuses));
    }

}
