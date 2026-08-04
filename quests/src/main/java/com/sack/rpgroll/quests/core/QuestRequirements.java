package com.sack.rpgroll.quests.core;

import java.util.List;

/**
 * Requisitos que un jugador debe cumplir para poder iniciar una quest.
 * Todos los campos son opcionales — un valor "vacío" (0, null, lista vacía)
 * significa que ese requisito no aplica.
 */
public record QuestRequirements(
        int level,
        String race,
        String playerClass,
        String profession,
        String trait,
        String permission,
        double money,
        List<ItemRequirement> items,
        List<String> completedQuests,
        String world,
        String region,
        String biome,
        String weather,
        int hourMin,
        int hourMax) {

    public QuestRequirements {
        items = items == null ? List.of() : List.copyOf(items);
        completedQuests = completedQuests == null ? List.of() : List.copyOf(completedQuests);
        hourMin = hourMin < 0 ? -1 : hourMin;
        hourMax = hourMax < 0 ? -1 : hourMax;
    }

    public static QuestRequirements none() {
        return new QuestRequirements(0, null, null, null, null, null, 0, List.of(), List.of(),
                null, null, null, null, -1, -1);
    }

    public boolean hasTimeRange() {
        return hourMin >= 0 && hourMax >= 0;
    }

}
