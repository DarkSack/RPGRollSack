package com.sack.rpgroll.crafting.proficiency;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Progreso de crafteo de un jugador, separado por {@code skillCategory} de
 * {@code CustomStation} (por ejemplo "forjado", "alquimia" — estaciones que
 * comparten categoría comparten el mismo progreso). Es la fuente del
 * {@code skillFactor} que antes estaba hardcodeado en {@code StationProcessingEngine}.
 */
public class PlayerProficiency {

    private final UUID playerId;
    private final Map<String, Double> xpByCategory;

    public PlayerProficiency(UUID playerId, Map<String, Double> xpByCategory) {
        this.playerId = playerId;
        this.xpByCategory = new HashMap<>(xpByCategory);
    }

    public static PlayerProficiency empty(UUID playerId) {
        return new PlayerProficiency(playerId, Map.of());
    }

    public UUID playerId() {
        return playerId;
    }

    public double xp(String category) {
        return xpByCategory.getOrDefault(category, 0.0);
    }

    /** @return el xp total de la categoría después de sumar. */
    public double addXp(String category, double amount) {
        double updated = xp(category) + Math.max(0, amount);
        xpByCategory.put(category, updated);
        return updated;
    }

    public Map<String, Double> xpByCategory() {
        return Map.copyOf(xpByCategory);
    }

}
