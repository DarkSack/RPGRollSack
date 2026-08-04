package com.sack.rpgroll.ascension.core;

import java.util.List;
import java.util.Map;

/**
 * Requisitos compartidos por evoluciones de raza, especializaciones de
 * clase y contenido secreto: nivel, prestigio, quests completadas, trait
 * requerido y reputación mínima con facciones. Todos opcionales.
 */
public record AscensionRequirements(
        int level,
        int prestige,
        String trait,
        List<String> completedQuests,
        Map<String, Integer> reputation) {

    public AscensionRequirements {
        completedQuests = completedQuests == null ? List.of() : List.copyOf(completedQuests);
        reputation = reputation == null ? Map.of() : Map.copyOf(reputation);
    }

    public static AscensionRequirements none() {
        return new AscensionRequirements(0, 0, null, List.of(), Map.of());
    }

}
