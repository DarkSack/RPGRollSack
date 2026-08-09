package com.sack.rpgroll.crafting.discovery;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Recetas de {@code CustomRecipe} que un jugador ya descubrió (aparecen en su Recipe Book). */
public class PlayerDiscovery {

    private final UUID playerId;
    private final Set<String> discoveredRecipeIds;

    public PlayerDiscovery(UUID playerId, Set<String> discoveredRecipeIds) {
        this.playerId = playerId;
        this.discoveredRecipeIds = new LinkedHashSet<>(discoveredRecipeIds);
    }

    public static PlayerDiscovery empty(UUID playerId) {
        return new PlayerDiscovery(playerId, Set.of());
    }

    public UUID playerId() {
        return playerId;
    }

    public boolean hasDiscovered(String recipeId) {
        return discoveredRecipeIds.contains(recipeId);
    }

    /** @return true si se agregó recién (false si ya lo tenía). */
    public boolean markDiscovered(String recipeId) {
        return discoveredRecipeIds.add(recipeId);
    }

    public Set<String> discoveredRecipeIds() {
        return Set.copyOf(discoveredRecipeIds);
    }

}
