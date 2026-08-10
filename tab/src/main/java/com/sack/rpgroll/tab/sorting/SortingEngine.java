package com.sack.rpgroll.tab.sorting;

import com.sack.rpgroll.tab.placeholder.PlaceholderEngine;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;

/** Construye un {@link Comparator} de jugadores a partir de una {@link SortingDefinition}. */
public class SortingEngine {

    private final PlaceholderEngine placeholderEngine;

    public SortingEngine(PlaceholderEngine placeholderEngine) {
        this.placeholderEngine = placeholderEngine;
    }

    public Comparator<Player> buildComparator(SortingDefinition definition) {

        Comparator<Player> comparator = null;

        for (SortRule rule : definition.rules()) {

            Comparator<Player> ruleComparator = buildRuleComparator(rule);
            comparator = comparator == null ? ruleComparator : comparator.thenComparing(ruleComparator);
        }

        return comparator == null ? Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER) : comparator;
    }

    private Comparator<Player> buildRuleComparator(SortRule rule) {

        Comparator<Player> comparator = rule.isPlaceholder() ? placeholderComparator(rule) : nativeComparator(rule);

        return rule.order() == SortOrder.DESC ? comparator.reversed() : comparator;
    }

    private Comparator<Player> nativeComparator(SortRule rule) {

        return switch (rule.field()) {
            case "ping" -> Comparator.comparingInt(Player::getPing);
            case "world" -> Comparator.comparing(p -> p.getWorld().getName(), String.CASE_INSENSITIVE_ORDER);
            case "online-time" -> Comparator.comparingInt(p -> p.getStatistic(Statistic.PLAY_ONE_MINUTE));
            case "permission" -> Comparator.comparingInt(p -> permissionScore(p, rule.values()));
            default -> Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private int permissionScore(Player player, List<String> permissions) {

        for (int i = 0; i < permissions.size(); i++) {
            if (player.hasPermission(permissions.get(i))) {
                return permissions.size() - i;
            }
        }

        return 0;
    }

    private Comparator<Player> placeholderComparator(SortRule rule) {

        if (rule.numeric()) {
            return Comparator.comparingDouble(p -> parseDouble(placeholderEngine.resolve(rule.placeholder(), p)));
        }

        return Comparator.comparing(
                p -> placeholderEngine.resolve(rule.placeholder(), p), String.CASE_INSENSITIVE_ORDER);
    }

    private double parseDouble(String value) {

        if (value == null) {
            return 0;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
