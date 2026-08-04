package com.sack.rpgroll.items.core;

import java.util.List;

/**
 * Requisitos para poder equipar/usar un ítem. Todos los campos son
 * opcionales — un valor "vacío" (0, null, lista vacía) significa que ese
 * requisito no aplica.
 */
public record ItemRequirements(
        int level,
        String race,
        String playerClass,
        String profession,
        String skill,
        String trait,
        String permission,
        double money,
        List<String> completedQuests) {

    public ItemRequirements {
        completedQuests = completedQuests == null ? List.of() : List.copyOf(completedQuests);
    }

    public static ItemRequirements none() {
        return new ItemRequirements(0, null, null, null, null, null, null, 0, List.of());
    }

}
