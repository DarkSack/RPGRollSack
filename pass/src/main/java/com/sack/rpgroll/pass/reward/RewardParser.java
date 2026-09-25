package com.sack.rpgroll.pass.reward;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Traduce el texto corto de {@link Reward} y avisa de lo que no entiende sin tumbar la carga. */
public final class RewardParser {

    private RewardParser() {
    }

    public static Reward parse(String raw) {

        String text = raw == null ? "" : raw.trim();
        int colon = text.indexOf(':');

        if (colon <= 0) {
            throw new IllegalArgumentException("falta el tipo (money:, key:, item:, material:, exp:, command:)");
        }

        String typeName = text.substring(0, colon).toLowerCase(Locale.ROOT);
        String rest = text.substring(colon + 1).trim();

        return switch (typeName) {
            case "money" -> new Reward(Reward.Type.MONEY, "", positive(rest), text);
            case "exp" -> new Reward(Reward.Type.EXP, "", positive(rest), text);
            case "command" -> {
                if (rest.isEmpty()) {
                    throw new IllegalArgumentException("command: sin comando");
                }
                yield new Reward(Reward.Type.COMMAND, rest.startsWith("/") ? rest.substring(1) : rest, 1, text);
            }
            case "key" -> keyed(Reward.Type.KEY, rest, text);
            case "item" -> keyed(Reward.Type.ITEM, rest, text);
            case "material" -> {
                Reward reward = keyed(Reward.Type.MATERIAL, rest, text);
                Material material = Material.matchMaterial(reward.key());
                if (material == null) {
                    throw new IllegalArgumentException("material desconocido: " + reward.key());
                }
                yield new Reward(Reward.Type.MATERIAL, material.name(), reward.amount(), text);
            }
            default -> throw new IllegalArgumentException("tipo desconocido: " + typeName);
        };
    }

    /** Parsea una lista y reporta cada entrada inválida por {@code warn}, saltándola. */
    public static List<Reward> parseAll(List<String> raws, String where, Consumer<String> warn) {

        List<Reward> rewards = new ArrayList<>();

        for (String raw : raws) {
            try {
                rewards.add(parse(raw));
            } catch (IllegalArgumentException e) {
                warn.accept(where + ": recompensa '" + raw + "' ignorada (" + e.getMessage() + ")");
            }
        }

        return List.copyOf(rewards);
    }

    private static Reward keyed(Reward.Type type, String rest, String raw) {

        String[] parts = rest.split(":");

        if (parts[0].isBlank()) {
            throw new IllegalArgumentException("falta el id");
        }

        int amount = parts.length > 1 ? positive(parts[1]) : 1;
        return new Reward(type, parts[0].trim(), amount, raw);
    }

    private static int positive(String value) {

        try {
            int amount = Integer.parseInt(value.trim());
            if (amount <= 0) {
                throw new IllegalArgumentException("la cantidad tiene que ser mayor que 0: " + value);
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("cantidad inválida: " + value);
        }
    }

}
