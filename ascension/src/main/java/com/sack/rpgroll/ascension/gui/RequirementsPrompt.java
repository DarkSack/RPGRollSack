package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.core.AscensionRequirements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Formato de línea de chat para {@link AscensionRequirements}:
 * nivel;prestigio;trait;quest1,quest2;faccion1=10,faccion2=5
 * Cualquier campo puede quedar vacío (ej. "0;0;-;;").
 */
public final class RequirementsPrompt {

    private RequirementsPrompt() {
    }

    public static String format(AscensionRequirements requirements) {

        String trait = requirements.trait() == null || requirements.trait().isBlank() ? "-" : requirements.trait();
        String quests = String.join(",", requirements.completedQuests());

        StringBuilder reputation = new StringBuilder();
        requirements.reputation().forEach((faction, amount) -> {
            if (reputation.length() > 0) {
                reputation.append(',');
            }
            reputation.append(faction).append('=').append(amount);
        });

        return requirements.level() + ";" + requirements.prestige() + ";" + trait + ";" + quests + ";" + reputation;
    }

    public static AscensionRequirements parse(String line) {

        String[] parts = line.split(";", -1);

        int level = parts.length > 0 && !parts[0].isBlank() ? Integer.parseInt(parts[0].trim()) : 0;
        int prestige = parts.length > 1 && !parts[1].isBlank() ? Integer.parseInt(parts[1].trim()) : 0;
        String trait = parts.length > 2 && !parts[2].isBlank() && !parts[2].trim().equals("-") ? parts[2].trim()
                : null;

        List<String> quests = parts.length > 3 && !parts[3].isBlank()
                ? List.of(parts[3].trim().split(","))
                : List.of();

        Map<String, Integer> reputation = new HashMap<>();

        if (parts.length > 4 && !parts[4].isBlank()) {
            for (String entry : parts[4].split(",")) {
                String[] kv = entry.split("=");
                if (kv.length == 2) {
                    reputation.put(kv[0].trim(), Integer.parseInt(kv[1].trim()));
                }
            }
        }

        return new AscensionRequirements(level, prestige, trait, quests, reputation);
    }

}
